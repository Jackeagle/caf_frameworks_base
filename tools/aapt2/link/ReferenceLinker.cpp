/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "link/ReferenceLinker.h"

#include "android-base/logging.h"
#include "androidfw/ResourceTypes.h"

#include "Diagnostics.h"
#include "ResourceTable.h"
#include "ResourceUtils.h"
#include "ResourceValues.h"
#include "ValueVisitor.h"
#include "link/Linkers.h"
#include "process/IResourceTableConsumer.h"
#include "process/SymbolTable.h"
#include "util/Util.h"
#include "xml/XmlUtil.h"

using android::StringPiece;

namespace aapt {

namespace {

/**
 * The ReferenceLinkerVisitor will follow all references and make sure they
 * point
 * to resources that actually exist, either in the local resource table, or as
 * external
 * symbols. Once the target resource has been found, the ID of the resource will
 * be assigned
 * to the reference object.
 *
 * NOTE: All of the entries in the ResourceTable must be assigned IDs.
 */
class ReferenceLinkerVisitor : public ValueVisitor {
 public:
  using ValueVisitor::Visit;

  ReferenceLinkerVisitor(const CallSite& callsite, IAaptContext* context, SymbolTable* symbols,
                         StringPool* string_pool, xml::IPackageDeclStack* decl)
      : callsite_(callsite),
        context_(context),
        symbols_(symbols),
        package_decls_(decl),
        string_pool_(string_pool) {}

  void Visit(Reference* ref) override {
    if (!ReferenceLinker::LinkReference(callsite_, ref, context_, symbols_, package_decls_)) {
      error_ = true;
    }
  }

  /**
   * We visit the Style specially because during this phase, values of
   * attributes are
   * all RawString values. Now that we are expected to resolve all symbols, we
   * can
   * lookup the attributes to find out which types are allowed for the
   * attributes' values.
   */
  void Visit(Style* style) override {
    if (style->parent) {
      Visit(&style->parent.value());
    }

    for (Style::Entry& entry : style->entries) {
      std::string err_str;

      // Transform the attribute reference so that it is using the fully
      // qualified package
      // name. This will also mark the reference as being able to see private
      // resources if
      // there was a '*' in the reference or if the package came from the
      // private namespace.
      Reference transformed_reference = entry.key;
      TransformReferenceFromNamespace(package_decls_,
                                      context_->GetCompilationPackage(),
                                      &transformed_reference);

      // Find the attribute in the symbol table and check if it is visible from
      // this callsite.
      const SymbolTable::Symbol* symbol = ReferenceLinker::ResolveAttributeCheckVisibility(
          transformed_reference, callsite_, symbols_, &err_str);
      if (symbol) {
        // Assign our style key the correct ID.
        // The ID may not exist.
        entry.key.id = symbol->id;

        // Try to convert the value to a more specific, typed value based on the
        // attribute it is set to.
        entry.value = ParseValueWithAttribute(std::move(entry.value), symbol->attribute.get());

        // Link/resolve the final value (mostly if it's a reference).
        entry.value->Accept(this);

        // Now verify that the type of this item is compatible with the
        // attribute it
        // is defined for. We pass `nullptr` as the DiagMessage so that this
        // check is
        // fast and we avoid creating a DiagMessage when the match is
        // successful.
        if (!symbol->attribute->Matches(entry.value.get(), nullptr)) {
          // The actual type of this item is incompatible with the attribute.
          DiagMessage msg(entry.key.GetSource());

          // Call the matches method again, this time with a DiagMessage so we
          // fill in the actual error message.
          symbol->attribute->Matches(entry.value.get(), &msg);
          context_->GetDiagnostics()->Error(msg);
          error_ = true;
        }

      } else {
        DiagMessage msg(entry.key.GetSource());
        msg << "style attribute '";
        ReferenceLinker::WriteResourceName(&msg, entry.key, transformed_reference);
        msg << "' " << err_str;
        context_->GetDiagnostics()->Error(msg);
        error_ = true;
      }
    }
  }

  bool HasError() { return error_; }

 private:
  DISALLOW_COPY_AND_ASSIGN(ReferenceLinkerVisitor);

  /**
   * Transform a RawString value into a more specific, appropriate value, based
   * on the
   * Attribute. If a non RawString value is passed in, this is an identity
   * transform.
   */
  std::unique_ptr<Item> ParseValueWithAttribute(std::unique_ptr<Item> value,
                                                const Attribute* attr) {
    if (RawString* raw_string = ValueCast<RawString>(value.get())) {
      std::unique_ptr<Item> transformed =
          ResourceUtils::TryParseItemForAttribute(*raw_string->value, attr);

      // If we could not parse as any specific type, try a basic STRING.
      if (!transformed && (attr->type_mask & android::ResTable_map::TYPE_STRING)) {
        util::StringBuilder string_builder;
        string_builder.Append(*raw_string->value);
        if (string_builder) {
          transformed = util::make_unique<String>(string_pool_->MakeRef(string_builder.ToString()));
        }
      }

      if (transformed) {
        return transformed;
      }
    }
    return value;
  }

  const CallSite& callsite_;
  IAaptContext* context_;
  SymbolTable* symbols_;
  xml::IPackageDeclStack* package_decls_;
  StringPool* string_pool_;
  bool error_ = false;
};

class EmptyDeclStack : public xml::IPackageDeclStack {
 public:
  EmptyDeclStack() = default;

  Maybe<xml::ExtractedPackage> TransformPackageAlias(
      const StringPiece& alias,
      const StringPiece& local_package) const override {
    if (alias.empty()) {
      return xml::ExtractedPackage{local_package.to_string(), true /* private */};
    }
    return {};
  }

 private:
  DISALLOW_COPY_AND_ASSIGN(EmptyDeclStack);
};

}  // namespace

/**
 * The symbol is visible if it is public, or if the reference to it is
 * requesting private access
 * or if the callsite comes from the same package.
 */
bool ReferenceLinker::IsSymbolVisible(const SymbolTable::Symbol& symbol,
                                      const Reference& ref,
                                      const CallSite& callsite) {
  if (!symbol.is_public && !ref.private_reference) {
    if (ref.name) {
      return callsite.resource.package == ref.name.value().package;
    } else if (ref.id && symbol.id) {
      return ref.id.value().package_id() == symbol.id.value().package_id();
    } else {
      return false;
    }
  }
  return true;
}

const SymbolTable::Symbol* ReferenceLinker::ResolveSymbol(const Reference& reference,
                                                          SymbolTable* symbols) {
  if (reference.name) {
    return symbols->FindByName(reference.name.value());
  } else if (reference.id) {
    return symbols->FindById(reference.id.value());
  } else {
    return nullptr;
  }
}

const SymbolTable::Symbol* ReferenceLinker::ResolveSymbolCheckVisibility(const Reference& reference,
                                                                         const CallSite& callsite,
                                                                         SymbolTable* symbols,
                                                                         std::string* out_error) {
  const SymbolTable::Symbol* symbol = ResolveSymbol(reference, symbols);
  if (!symbol) {
    if (out_error) *out_error = "not found";
    return nullptr;
  }

  if (!IsSymbolVisible(*symbol, reference, callsite)) {
    if (out_error) *out_error = "is private";
    return nullptr;
  }
  return symbol;
}

const SymbolTable::Symbol* ReferenceLinker::ResolveAttributeCheckVisibility(
    const Reference& reference, const CallSite& callsite, SymbolTable* symbols,
    std::string* out_error) {
  const SymbolTable::Symbol* symbol =
      ResolveSymbolCheckVisibility(reference, callsite, symbols, out_error);
  if (!symbol) {
    return nullptr;
  }

  if (!symbol->attribute) {
    if (out_error) *out_error = "is not an attribute";
    return nullptr;
  }
  return symbol;
}

Maybe<xml::AaptAttribute> ReferenceLinker::CompileXmlAttribute(const Reference& reference,
                                                               const CallSite& callsite,
                                                               SymbolTable* symbols,
                                                               std::string* out_error) {
  const SymbolTable::Symbol* symbol =
      ResolveAttributeCheckVisibility(reference, callsite, symbols, out_error);
  if (!symbol) {
    return {};
  }

  if (!symbol->attribute) {
    if (out_error) *out_error = "is not an attribute";
    return {};
  }
  return xml::AaptAttribute(*symbol->attribute, symbol->id);
}

void ReferenceLinker::WriteResourceName(DiagMessage* out_msg,
                                        const Reference& orig,
                                        const Reference& transformed) {
  CHECK(out_msg != nullptr);

  if (orig.name) {
    *out_msg << orig.name.value();
    if (transformed.name.value() != orig.name.value()) {
      *out_msg << " (aka " << transformed.name.value() << ")";
    }
  } else {
    *out_msg << orig.id.value();
  }
}

bool ReferenceLinker::LinkReference(const CallSite& callsite, Reference* reference,
                                    IAaptContext* context, SymbolTable* symbols,
                                    xml::IPackageDeclStack* decls) {
  CHECK(reference != nullptr);
  if (!reference->name && !reference->id) {
    // This is @null.
    return true;
  }

  Reference transformed_reference = *reference;
  TransformReferenceFromNamespace(decls, context->GetCompilationPackage(), &transformed_reference);

  std::string err_str;
  const SymbolTable::Symbol* s =
      ResolveSymbolCheckVisibility(transformed_reference, callsite, symbols, &err_str);
  if (s) {
    // The ID may not exist. This is fine because of the possibility of building
    // against libraries without assigned IDs.
    // Ex: Linking against own resources when building a static library.
    reference->id = s->id;
    return true;
  }

  DiagMessage error_msg(reference->GetSource());
  error_msg << "resource ";
  WriteResourceName(&error_msg, *reference, transformed_reference);
  error_msg << " " << err_str;
  context->GetDiagnostics()->Error(error_msg);
  return false;
}

bool ReferenceLinker::Consume(IAaptContext* context, ResourceTable* table) {
  EmptyDeclStack decl_stack;
  bool error = false;
  for (auto& package : table->packages) {
    for (auto& type : package->types) {
      for (auto& entry : type->entries) {
        // Symbol state information may be lost if there is no value for the
        // resource.
        if (entry->symbol_status.state != SymbolState::kUndefined &&
            entry->values.empty()) {
          context->GetDiagnostics()->Error(
              DiagMessage(entry->symbol_status.source)
              << "no definition for declared symbol '"
              << ResourceNameRef(package->name, type->type, entry->name)
              << "'");
          error = true;
        }

        CallSite callsite = {ResourceNameRef(package->name, type->type, entry->name)};
        ReferenceLinkerVisitor visitor(callsite, context, context->GetExternalSymbols(),
                                       &table->string_pool, &decl_stack);

        for (auto& config_value : entry->values) {
          config_value->value->Accept(&visitor);
        }

        if (visitor.HasError()) {
          error = true;
        }
      }
    }
  }
  return !error;
}

}  // namespace aapt

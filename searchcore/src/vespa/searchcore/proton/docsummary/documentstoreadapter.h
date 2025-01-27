// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/searchsummary/docsummary/docsumstore.h>
#include <vespa/searchlib/docstore/idocumentstore.h>

namespace proton {

class DocumentStoreAdapter : public search::docsummary::IDocsumStore
{
private:
    const search::IDocumentStore           & _docStore;
    const document::DocumentTypeRepo       & _repo;

public:
    DocumentStoreAdapter(const search::IDocumentStore &docStore,
                         const document::DocumentTypeRepo &repo);
    ~DocumentStoreAdapter();

    uint32_t getNumDocs() const override { return _docStore.getDocIdLimit(); }
    std::unique_ptr<const search::docsummary::IDocsumStoreDocument> getMappedDocsum(uint32_t docId) override;
};

} // namespace proton


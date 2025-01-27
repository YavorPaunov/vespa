// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <memory>

namespace search::docsummary {

class IDocsumStoreDocument;

/**
 * Interface for object able to fetch docsum blobs based on local
 * document id.
 **/
class IDocsumStore
{
public:
    /**
     * Convenience typedef.
     */
    typedef std::unique_ptr<IDocsumStore> UP;

    /**
     * Destructor.  No cleanup needed for base class.
     */
    virtual ~IDocsumStore() = default;

    /**
     * @return total number of documents.
     **/
    virtual uint32_t getNumDocs() const = 0;

    /**
     * Get a reference to a docsum blob in memory.  The docsum store
     * owns the memory (which is either mmap()ed or from a memory-based
     * index of some kind).
     *
     * @return unique pointer to interface class providing access to document
     * @param docid local document id
     **/
    virtual std::unique_ptr<const IDocsumStoreDocument> getMappedDocsum(uint32_t docid) = 0;
};

}

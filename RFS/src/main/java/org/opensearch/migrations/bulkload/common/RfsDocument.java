package org.opensearch.migrations.bulkload.common;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opensearch.migrations.transform.IJsonTransformer;

import lombok.AllArgsConstructor;

/** 
 * This class represents a document within RFS during the Reindexing process.  It tracks:
 * * The original Lucene context of the document (Lucene segment and document identifiers)
 * * The original Elasticsearch/OpenSearch context of the document (Index and Shard)
 * * The final shape of the document as needed for reindexing
 */
@AllArgsConstructor
public class RfsDocument {
    // The Lucene index doc number of the document (global over shard / lucene-index)
    public final int luceneDocNumber;

    // The Elasticsearch/OpenSearch document to be reindexed
    public final BulkDocSection document;

    public static RfsDocument fromLuceneDocument(RfsLuceneDocument doc, String indexName) {
        return new RfsDocument(
            doc.luceneDocNumber,
            new BulkDocSection(
                doc.id,
                indexName,
                doc.type,
                doc.source,
                doc.routing
            )
        );
    }

    @SuppressWarnings("unchecked")
    public static List<RfsDocument> transform(IJsonTransformer transformer, List<RfsDocument> docs) {
        var listOfDocMaps = docs.stream().map(doc -> doc.document.toMap())
                .toList();

        // Use the first luceneDocNumber in the batch to associate with all returned objects
        var luceneDocNumber = docs.stream().findFirst().map(doc -> doc.luceneDocNumber).orElseThrow(
                () -> new IllegalArgumentException("Expected non-empty list of docs, but was empty.")
        );

        var transformedObject = transformer.transformJson(listOfDocMaps);
        if (transformedObject instanceof List) {
            var transformedList = (List<Map<String, Object>>) transformedObject;
            return transformedList.stream()
                .map(item -> new RfsDocument(
                    luceneDocNumber,
                    BulkDocSection.fromMap(item)
                ))
                .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException(
                "Unsupported transformed document type: " + transformedObject.getClass().getName()
            );
        }
    }
}

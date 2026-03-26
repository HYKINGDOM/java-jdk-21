package com.skillserver.service;

import com.skillserver.config.SkillServerProperties;
import com.skillserver.domain.entity.SkillCurrentDocEntity;
import com.skillserver.domain.entity.SkillEntity;
import com.skillserver.service.model.SearchHit;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import static org.apache.lucene.document.Field.Store.YES;

@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final SkillServerProperties properties;

    private Path indexPath;

    @PostConstruct
    public void init() {
        this.indexPath = properties.getWorkspace().getSearchIndexDir();
    }

    public synchronized void upsert(SkillCurrentDocEntity doc, SkillEntity skill) {
        try (Directory directory = FSDirectory.open(indexPath);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()))) {
            writer.deleteDocuments(new Term("skillUid", skill.getSkillUid()));
            Document luceneDocument = new Document();
            luceneDocument.add(new StringField("skillUid", skill.getSkillUid(), YES));
            luceneDocument.add(new StringField("sourceType", skill.getSourceType().name(), YES));
            luceneDocument.add(new StringField("status", skill.getStatus().name(), YES));
            luceneDocument.add(new TextField("title", doc.getTitle(), YES));
            luceneDocument.add(new TextField("summary", defaultValue(doc.getSummary()), YES));
            luceneDocument.add(new TextField("body", defaultValue(doc.getBodyPlaintext()), YES));
            luceneDocument.add(new TextField("tags", defaultValue(doc.getTagsJson()), YES));
            long updatedAt = doc.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            luceneDocument.add(new LongPoint("updatedAt", updatedAt));
            luceneDocument.add(new StoredField("updatedAtStored", updatedAt));
            writer.addDocument(luceneDocument);
            writer.commit();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to update search index for skill: " + skill.getSkillUid(), ex);
        }
    }

    public synchronized void delete(String skillUid) {
        try (Directory directory = FSDirectory.open(indexPath);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()))) {
            writer.deleteDocuments(new Term("skillUid", skillUid));
            writer.commit();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete search index for skill: " + skillUid, ex);
        }
    }

    public synchronized List<SearchHit> search(String queryText, int limit) {
        if (queryText == null || queryText.isBlank()) {
            return List.of();
        }
        try (Directory directory = FSDirectory.open(indexPath)) {
            if (!DirectoryReader.indexExists(directory)) {
                return List.of();
            }
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    new String[]{"title", "summary", "body", "tags"},
                    new StandardAnalyzer()
                );
                Query query = parser.parse(MultiFieldQueryParser.escape(queryText));
                ScoreDoc[] scoreDocs = searcher.search(query, limit).scoreDocs;
                List<SearchHit> hits = new ArrayList<>(scoreDocs.length);
                for (ScoreDoc scoreDoc : scoreDocs) {
                    Document document = searcher.storedFields().document(scoreDoc.doc);
                    hits.add(new SearchHit(document.get("skillUid"), scoreDoc.score));
                }
                return hits;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to search skills", ex);
        }
    }

    public synchronized boolean exists(String skillUid) {
        try (Directory directory = FSDirectory.open(indexPath)) {
            if (!DirectoryReader.indexExists(directory)) {
                return false;
            }
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                return searcher.count(new TermQuery(new Term("skillUid", skillUid))) > 0;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to check search index", ex);
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}

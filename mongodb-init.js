// MongoDB Initialization Script for RDSS

// Switch to the RDSS documents database
db = db.getSiblingDB('rdss_documents');

// Create collections
db.createCollection('documents');
db.createCollection('embeddings');
db.createCollection('processing_queue');
db.createCollection('metadata');

// Create indexes for documents collection
db.documents.createIndex({ "id": 1 }, { unique: true });
db.documents.createIndex({ "title": "text", "abstract": "text" });
db.documents.createIndex({ "authors": 1 });
db.documents.createIndex({ "year": 1 });
db.documents.createIndex({ "type": 1 });
db.documents.createIndex({ "tags": 1 });
db.documents.createIndex({ "createdAt": 1 });
db.documents.createIndex({ "updatedAt": 1 });

// Create indexes for embeddings collection
db.embeddings.createIndex({ "documentId": 1 });
db.embeddings.createIndex({ "chunkIndex": 1 });
db.embeddings.createIndex({ "documentId": 1, "chunkIndex": 1 }, { unique: true });

// Create indexes for processing_queue collection
db.processing_queue.createIndex({ "status": 1 });
db.processing_queue.createIndex({ "createdAt": 1 });
db.processing_queue.createIndex({ "priority": 1 });
db.processing_queue.createIndex({ "documentId": 1 }, { unique: true });

// Create sample document metadata
db.metadata.insertOne({
    "_id": "system_config",
    "version": "1.0.0",
    "createdAt": new Date(),
    "lastUpdated": new Date(),
    "settings": {
        "maxDocumentSize": 104857600, // 100MB
        "supportedFormats": ["pdf", "docx", "txt", "md"],
        "chunkSize": 1000,
        "chunkOverlap": 200
    }
});

// Insert sample documents
db.documents.insertMany([
    {
        "id": "doc-fog-architecture-2023",
        "title": "Fog Computing Architecture for Military AI Applications",
        "type": "ACADEMIC_PAPER",
        "authors": ["Smith, J.", "Johnson, K.", "Williams, M."],
        "year": 2023,
        "venue": "IEEE Military Communications Conference",
        "doi": "10.1109/MILCOM52395.2023.1234567",
        "abstract": "This paper presents a novel fog computing architecture designed specifically for military AI applications...",
        "content": "Full text content would be stored here...",
        "filePath": "/documents/fog-architecture-2023.pdf",
        "fileSize": 2048576,
        "tags": ["fog-computing", "military-ai", "distributed-systems"],
        "createdAt": new Date(),
        "updatedAt": new Date(),
        "processingStatus": "completed",
        "extractedEntities": {
            "concepts": ["fog computing", "military AI", "distributed systems"],
            "methods": ["distributed inference", "edge processing"],
            "datasets": [],
            "researchAreas": ["fog computing", "military AI"]
        }
    },
    {
        "id": "doc-edge-optimization-2023",
        "title": "Model Optimization Techniques for Edge AI Deployment",
        "type": "ACADEMIC_PAPER",
        "authors": ["Chen, L.", "Brown, A.", "Davis, R."],
        "year": 2023,
        "venue": "NeurIPS",
        "doi": "10.5555/3601234.5678901",
        "abstract": "This paper explores various techniques for optimizing machine learning models for deployment on resource-constrained edge devices...",
        "content": "Full text content would be stored here...",
        "filePath": "/documents/edge-optimization-2023.pdf",
        "fileSize": 1536789,
        "tags": ["model-optimization", "edge-computing", "quantization"],
        "createdAt": new Date(),
        "updatedAt": new Date(),
        "processingStatus": "completed",
        "extractedEntities": {
            "concepts": ["model optimization", "edge computing"],
            "methods": ["quantization", "pruning", "knowledge distillation"],
            "datasets": ["ImageNet", "CIFAR-10"],
            "researchAreas": ["machine learning", "edge computing"]
        }
    }
]);

// Insert sample embeddings
db.embeddings.insertMany([
    {
        "documentId": "doc-fog-architecture-2023",
        "chunkIndex": 0,
        "chunkText": "Fog computing represents a paradigm shift in how we approach distributed AI systems...",
        "embedding": [0.1, 0.2, 0.3, /* ... 384 dimensions total ... */],
        "metadata": {
            "chunkSize": 1000,
            "startIndex": 0,
            "endIndex": 1000,
            "createdAt": new Date()
        }
    },
    {
        "documentId": "doc-edge-optimization-2023",
        "chunkIndex": 0,
        "chunkText": "Model optimization is crucial for deploying AI models on edge devices with limited computational resources...",
        "embedding": [0.4, 0.5, 0.6, /* ... 384 dimensions total ... */],
        "metadata": {
            "chunkSize": 1000,
            "startIndex": 0,
            "endIndex": 1000,
            "createdAt": new Date()
        }
    }
]);

// Create processing queue sample entries
db.processing_queue.insertMany([
    {
        "documentId": "doc-new-paper-2024",
        "status": "pending",
        "priority": "high",
        "filePath": "/documents/new-paper-2024.pdf",
        "metadata": {
            "title": "New Research Paper 2024",
            "type": "ACADEMIC_PAPER",
            "uploadedAt": new Date()
        },
        "createdAt": new Date(),
        "retryCount": 0,
        "maxRetries": 3
    }
]);

print("MongoDB initialization completed successfully");

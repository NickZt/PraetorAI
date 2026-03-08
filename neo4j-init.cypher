// Neo4j Initialization Script for RDSS
// This script sets up constraints, indexes, and initial data

// Create constraints for uniqueness
CREATE CONSTRAINT concept_id_unique IF NOT EXISTS FOR (c:Concept) REQUIRE c.id IS UNIQUE;
CREATE CONSTRAINT sourcedocument_id_unique IF NOT EXISTS FOR (sd:SourceDocument) REQUIRE sd.id IS UNIQUE;
CREATE CONSTRAINT experiment_id_unique IF NOT EXISTS FOR (e:Experiment) REQUIRE e.id IS UNIQUE;
CREATE CONSTRAINT workdocument_id_unique IF NOT EXISTS FOR (wd:WorkDocument) REQUIRE wd.id IS UNIQUE;
CREATE CONSTRAINT researcharea_id_unique IF NOT EXISTS FOR (ra:ResearchArea) REQUIRE ra.id IS UNIQUE;
CREATE CONSTRAINT conceptversion_id_unique IF NOT EXISTS FOR (cv:ConceptVersion) REQUIRE cv.id IS UNIQUE;

// Create indexes for performance
CREATE INDEX concept_name_index IF NOT EXISTS FOR (c:Concept) ON (c.name);
CREATE INDEX concept_maturity_index IF NOT EXISTS FOR (c:Concept) ON (c.maturity);
CREATE INDEX concept_status_index IF NOT EXISTS FOR (c:Concept) ON (c.status);
CREATE INDEX concept_lastupdated_index IF NOT EXISTS FOR (c:Concept) ON (c.lastUpdated);

CREATE INDEX sourcedocument_title_index IF NOT EXISTS FOR (sd:SourceDocument) ON (sd.title);
CREATE INDEX sourcedocument_type_index IF NOT EXISTS FOR (sd:SourceDocument) ON (sd.type);
CREATE INDEX sourcedocument_year_index IF NOT EXISTS FOR (sd:SourceDocument) ON (sd.year);
CREATE INDEX sourcedocument_doi_index IF NOT EXISTS FOR (sd:SourceDocument) ON (sd.doi);

CREATE INDEX experiment_status_index IF NOT EXISTS FOR (e:Experiment) ON (e.status);
CREATE INDEX experiment_date_index IF NOT EXISTS FOR (e:Experiment) ON (e.date);

CREATE INDEX workdocument_status_index IF NOT EXISTS FOR (wd:WorkDocument) ON (wd.status);
CREATE INDEX workdocument_type_index IF NOT EXISTS FOR (wd:WorkDocument) ON (wd.type);

CREATE INDEX researcharea_name_index IF NOT EXISTS FOR (ra:ResearchArea) ON (ra.name);

CREATE INDEX conceptversion_conceptid_index IF NOT EXISTS FOR (cv:ConceptVersion) ON (cv.conceptId);
CREATE INDEX conceptversion_version_index IF NOT EXISTS FOR (cv:ConceptVersion) ON (cv.version);

// Create full-text search indexes
CALL db.index.fulltext.createNodeIndex("conceptFulltext", ["Concept"], ["name", "description"]);
CALL db.index.fulltext.createNodeIndex("documentFulltext", ["SourceDocument"], ["title", "abstract"]);
CALL db.index.fulltext.createNodeIndex("experimentFulltext", ["Experiment"], ["name", "hypothesis", "resultsSummary"]);

// Create initial research areas
MERGE (ai:ResearchArea {id: 'ai', name: 'Artificial Intelligence', description: 'Core AI research and applications'});
MERGE (ml:ResearchArea {id: 'ml', name: 'Machine Learning', description: 'Machine learning algorithms and techniques'});
MERGE (dl:ResearchArea {id: 'dl', name: 'Deep Learning', description: 'Deep neural networks and architectures'});
MERGE (fc:ResearchArea {id: 'fc', name: 'Fog Computing', description: 'Distributed computing at the network edge'});
MERGE (ec:ResearchArea {id: 'ec', name: 'Edge Computing', description: 'Computing at the edge of networks'});
MERGE (mil:ResearchArea {id: 'mil', name: 'Military AI', description: 'AI applications for military and defense'});
MERGE (ds:ResearchArea {id: 'ds', name: 'Distributed Systems', description: 'Distributed computing systems and architectures'});

// Create research area hierarchy
MERGE (ml) -[:SUBAREA_OF]-> (ai);
MERGE (dl) -[:SUBAREA_OF]-> (ml);
MERGE (ec) -[:SUBAREA_OF]-> (fc);
MERGE (mil) -[:SUBAREA_OF]-> (ai);

// Sample seed concepts for demonstration
MERGE (di:Concept {
    id: 'concept-distributed-inference',
    name: 'Distributed Inference on Fog Nodes',
    description: 'Distributing machine learning inference across fog computing nodes to reduce latency and improve scalability in tactical environments.',
    maturity: 'DEVELOPING',
    status: 'ACTIVE',
    firstMentioned: datetime(),
    lastUpdated: datetime(),
    currentVersion: 1,
    tags: ['fog-computing', 'ml-inference', 'military-ai', 'distributed-systems']
});

MERGE (mo:Concept {
    id: 'concept-model-optimization',
    name: 'Model Optimization for Edge Deployment',
    description: 'Techniques for optimizing machine learning models to run efficiently on resource-constrained edge devices.',
    maturity: 'MATURE',
    status: 'ACTIVE',
    firstMentioned: datetime(),
    lastUpdated: datetime(),
    currentVersion: 2,
    tags: ['model-optimization', 'edge-computing', 'quantization', 'pruning']
});

// Create relationships between concepts
MERGE (di) -[:BUILDS_ON]-> (mo);

// Associate concepts with research areas
MERGE (di) -[:BELONGS_TO]-> (fc);
MERGE (di) -[:BELONGS_TO]-> (mil);
MERGE (mo) -[:BELONGS_TO]-> (ec);
MERGE (mo) -[:BELONGS_TO]-> (ml);

// Create sample experiment
MERGE (exp:Experiment {
    id: 'exp-latency-optimization-2024',
    name: 'Latency Optimization in Distributed Inference',
    date: datetime(),
    hypothesis: 'Distributed inference across fog nodes can reduce latency by 40% compared to centralized processing.',
    methodology: 'Implemented distributed inference pipeline across 3 fog nodes, measured end-to-end latency',
    resultsSummary: 'Achieved 45% latency reduction with 95% accuracy maintained',
    conclusion: 'Hypothesis validated, distributed inference shows significant promise for tactical applications',
    codeRepo: 'https://github.com/tactorder/distributed-inference',
    status: 'COMPLETED'
});

// Create experiment-concept relationships
MERGE (exp) -[:VALIDATES]-> (di);
MERGE (exp) -[:VALIDATES]-> (mo);

// Create sample source document
MERGE (doc:SourceDocument {
    id: 'doc-fog-architecture-2023',
    title: 'Fog Computing Architecture for Military AI Applications',
    type: 'ACADEMIC_PAPER',
    authors: ['Smith, J.', 'Johnson, K.', 'Williams, M.'],
    year: 2023,
    venue: 'IEEE Military Communications Conference',
    doi: '10.1109/MILCOM52395.2023.1234567',
    abstract: 'This paper presents a novel fog computing architecture designed specifically for military AI applications...',
    tags: ['fog-computing', 'military-ai', 'distributed-systems']
});

// Create document-concept relationships
MERGE (di) -[:INSPIRED_BY]-> (doc);
MERGE (doc) -[:BELONGS_TO]-> (fc);
MERGE (doc) -[:BELONGS_TO]-> (mil);

// Create sample work document
MERGE (work:WorkDocument {
    id: 'work-nato-proposal-2024',
    title: 'NATO Proposal: Fog-Based AI for Tactical Decision Support',
    type: 'GRANT_PROPOSAL',
    status: 'DRAFT',
    filePath: './documents/nato-proposal-2024.md',
    deadline: date('2024-06-30')
});

// Create work document relationships
MERGE (work) -[:APPLIES]-> (di);
MERGE (work) -[:CITES]-> (doc);

// Create initial concept versions
MERGE (v1:ConceptVersion {
    id: 'version-di-1',
    conceptId: 'concept-distributed-inference',
    version: 1,
    date: datetime(),
    description: 'Initial concept of distributed inference on fog nodes',
    keyInsights: ['Fog nodes can reduce inference latency', 'Distributed processing improves scalability'],
    changes: ['Initial formulation'],
    reason: 'Concept inception'
});

MERGE (v2:ConceptVersion {
    id: 'version-mo-1',
    conceptId: 'concept-model-optimization',
    version: 1,
    date: datetime().minus(days, 180),
    description: 'Basic model optimization techniques',
    keyInsights: ['Quantization reduces model size', 'Pruning improves inference speed'],
    changes: ['Initial research on optimization'],
    reason: 'Research phase'
});

MERGE (v3:ConceptVersion {
    id: 'version-mo-2',
    conceptId: 'concept-model-optimization',
    version: 2,
    date: datetime().minus(days, 30),
    description: 'Advanced optimization techniques for edge deployment',
    keyInsights: ['Combined quantization and pruning', 'Hardware-aware optimization'],
    changes: ['Added hardware-specific optimizations', 'Improved accuracy-retention techniques'],
    reason: 'Experimental validation and refinement'
});

// Create version relationships
MERGE (v2) -[:EVOLVED_FROM]-> (v1);
MERGE (v3) -[:EVOLVED_FROM]-> (v2);

// Connect versions to concepts
MERGE (di) -[:HAS_VERSION]-> (v1);
MERGE (mo) -[:HAS_VERSION]-> (v2);
MERGE (mo) -[:HAS_VERSION]-> (v3);

// Output summary
RETURN 'RDSS Neo4j database initialized successfully' as message;

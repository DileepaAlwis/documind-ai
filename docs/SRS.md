# Software Requirements Specification: DocuMind AI

Version: 0.1 (draft)
Status: Approved for development start
Related documents: [ARCHITECTURE.md](ARCHITECTURE.md), [ER_DIAGRAM.md](ER_DIAGRAM.md), [PROJECT_PLAN.md](PROJECT_PLAN.md)

## Intended audience and purpose

This document defines what DocuMind AI must do and how well it must do it. It is written for the developer building the system, for anyone reviewing the project as a portfolio piece, and for future contributors. Design decisions (how the system is built) are described in `ARCHITECTURE.md`; this document describes the requirements only.

---

## 1. Problem statement

People spend a lot of time reading, searching and organizing long documents. DocuMind AI lets a user upload documents, ask questions that are answered **from those documents with citations**, summarize them, and generate study material.

## 2. Target users

University students, researchers, lecturers, professionals, and organizations that work with large document collections.

## 3. Functional requirements (FR)

| ID | Area | Requirement | Priority |
|---|---|---|---|
| FR-1 | Auth | Register, login, JWT access + refresh tokens, logout, BCrypt hashing | Must |
| FR-2 | Users | View/update own profile; roles USER and ADMIN | Must |
| FR-3 | Workspaces | CRUD workspaces, search, per-workspace statistics | Must |
| FR-4 | Documents | Upload PDF/DOCX/TXT, validation, size limit, status, delete, search, metadata | Must |
| FR-5 | Documents | Drag-and-drop, upload progress, preview | Must |
| FR-6 | Documents | Detect image-only PDFs and report "no extractable text"; optional OCR later | Should |
| FR-7 | Processing | Extract, clean, chunk, embed, index; failed/retryable states; re-index | Must |
| FR-8 | Q&A | Ask over one document or a whole workspace; answers grounded in retrieved chunks | Must |
| FR-9 | Q&A | Citations (document name, page, chunk) validated against retrieved chunks; "not enough information" behavior | Must |
| FR-10 | Q&A | Conversation history and follow-up questions | Must |
| FR-11 | Summaries | Short, detailed, key points, definitions, concepts, chapter-wise where structure allows | Must (short/detailed/key points), Should (rest) |
| FR-12 | Learning | MCQs, Q&A pairs, flashcards, revision notes, quizzes, grounded in selected documents | Must (MCQ, flashcards), Should (rest) |
| FR-13 | Search | Semantic search plus keyword search; filter by workspace and file type; recent documents | Must |
| FR-14 | Dashboard | Totals, processed/failed counts, questions asked, recent activity, usage stats | Must |
| FR-15 | Admin | User management and status, system statistics, processing and failed-AI-request monitoring, config | Should |
| FR-16 | Evaluation | Measured chunking, embedding, retrieval and RAG experiments with a 30-50 item dataset | Must |

### 3.1 Scope cut line

Time is limited to one month. Order of importance:

1. **Core (must be finished and tested):** FR-1 to FR-5, FR-7 to FR-10, FR-13, FR-16.
2. **Next:** FR-11, FR-12, FR-14.
3. **If time allows:** FR-6 OCR, FR-15 admin extras, dark mode, streaming responses, Docker.

If a week slips, features are cut from the "if time allows" list. Tests and evaluation are never cut.

## 4. Non-functional requirements (NFR)

| ID | Requirement |
|---|---|
| NFR-1 Security | BCrypt, JWT, RBAC, ownership checks in the backend, private file storage, validated uploads, secrets only in environment variables, rate limiting, safe error messages |
| NFR-2 Privacy | A user can never read another user's documents, chunks, vectors or conversations. Every vector search is filtered by authorized IDs |
| NFR-3 Reliability | Partial failures leave a visible FAILED or retryable state; deletion and re-indexing are consistent across MySQL, Qdrant and disk |
| NFR-4 Performance | Documents are processed in batches, never fully loaded when large; processing is asynchronous. Answers on CPU may take 10-40 seconds, so the UI shows progress |
| NFR-5 Maintainability | Layered architecture, replaceable LLM provider, replaceable embedding model, configuration through environment variables |
| NFR-6 Usability | Responsive, accessible, loading/empty/error states, confirmation dialogs, toast notifications |
| NFR-7 Honesty | No fabricated citations, test results or metrics anywhere in the project or its documentation |
| NFR-8 Portability | Runs locally with no Docker and no paid API |

## 5. Constraints and assumptions

- Development machine: Windows 11, Intel Core i5-1335U, 16 GB RAM, no GPU.
- LLM: Ollama with `llama3.2:3b` (local, free).
- Embeddings: Sentence Transformers `all-MiniLM-L6-v2` (384 dimensions), configurable.
- Vector store: Qdrant in local persistent mode (single process).
- Relational database: MySQL 8.
- Maximum upload size: 25 MB (configurable).
- Supported file types: PDF, DOCX, TXT.
- Delivery time: approximately one month.

## 6. Out of scope for the first release

- Real-time multi-user collaboration on documents.
- Mobile native applications.
- Fine-tuning language models.
- Paid cloud LLM APIs (a provider abstraction allows them later).

## 7. Acceptance criteria for the core system

1. A new user can register, log in, create a workspace and upload a PDF, DOCX and TXT file.
2. Each uploaded document reaches status READY, or shows FAILED/NO_TEXT with a clear reason and a retry option.
3. A question about a document returns an answer with sources (document name, page where available) that all come from chunks actually retrieved.
4. A question with no support in the documents returns an honest "not enough information" response.
5. User A cannot read, search or ask questions about user B's documents (verified by automated tests).
6. Deleting a document removes its file, chunks and vectors.
7. Evaluation results in the report come from real runs on the stored evaluation dataset.

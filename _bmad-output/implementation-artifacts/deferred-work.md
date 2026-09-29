
- source_spec: `D:\FPT_Ky5\SWP\bmad swp\_bmad-output\implementation-artifacts\spec-sprint1-api-contract.md`
  summary: Thêm POST /notifications/{id}/read — mark-read từng item cho bell (hiện chỉ có mark-all-read).
  evidence: FR-34 nguyên văn chỉ đòi mark-all-read nên không phá Sprint 1, nhưng UX click-một-notification không tự chuyển read → unread-count stale đến khi mark-all. Thêm endpoint = mở rộng frozen Intent của contract Sprint 1, cần renegotiate khi Sprint 2 mở congelen lại.

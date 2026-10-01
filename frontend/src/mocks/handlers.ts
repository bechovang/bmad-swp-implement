import { http, HttpResponse } from 'msw'

// MSW 2.x handlers, developed against contracts/openapi.yaml (AD-2) while the
// backend catches up. Story 1.1 ships only this envelope-convention example;
// real handlers arrive with the epic 1 stories.
export const handlers = [
  http.get('/api/v1/units', () =>
    // ListEnvelope shape per AD-8: {items, page, pageSize, total}, page 1-based.
    HttpResponse.json({
      items: [],
      page: 1,
      pageSize: 25,
      total: 0,
    }),
  ),
]

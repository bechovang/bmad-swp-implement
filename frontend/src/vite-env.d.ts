/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Opt into MSW mock-first development: set VITE_ENABLE_MSW=true in .env.local */
  readonly VITE_ENABLE_MSW?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

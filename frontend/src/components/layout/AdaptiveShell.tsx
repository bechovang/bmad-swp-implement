import { Outlet } from 'react-router-dom'
import { TopBar } from './TopBar'
import { SafeQueryClientProvider } from './SafeQueryClientProvider'

export function AdaptiveShell() {
  return (
    <SafeQueryClientProvider>
      <div className="min-h-screen bg-sh-app-bg flex flex-col text-sh-ink">
        <TopBar />
        <main className="flex-1 px-sh-page-x py-6 max-w-[1440px] w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </SafeQueryClientProvider>
  )
}


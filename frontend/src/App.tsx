// Scaffold home (story 1.1). This is intentionally plain markup - no shared
// components or design tokens exist before the week-1 decisions land (gate:
// story 1.4). The role landings mirror contracts/routes.yaml.

const ROLE_LANDINGS = [
  { role: 'Customer', landing: '/units', screen: 'Browse Units' },
  { role: 'Staff', landing: '/tasks', screen: 'Task Board' },
  { role: 'Facility Manager', landing: '/overview', screen: 'Facility Overview' },
  { role: 'Business Ops', landing: '/business-overview', screen: 'Business Overview' },
  { role: 'System Administrator', landing: '/users', screen: 'User Management' },
] as const

const DEMO_UNITS = [
  { code: 'S-3', label: 'S-3', detail: '5 m2 - Preparing' },
  { code: 'M-2', label: 'M-2', detail: '8 m2 - Maintenance' },
  { code: 'M-5', label: 'M-5', detail: '8 m2 - Available' },
] as const

function App() {
  return (
    <main>
      <h1>StorageHub</h1>
      <p>
        Frontend scaffold (story 1.1). API calls go through the dev proxy
        (<code>/api</code> to <code>localhost:8080</code>); the contract lives in{' '}
        <code>contracts/openapi.yaml</code>.
      </p>

      <section aria-labelledby="role-landings-heading">
        <h2 id="role-landings-heading">Role landings (contracts/routes.yaml)</h2>
        <ul>
          {ROLE_LANDINGS.map(({ role, landing, screen }) => (
            <li key={role}>
              <strong>{role}</strong> - {screen} at <code>{landing}</code>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="demo-units-heading">
        <h2 id="demo-units-heading">Demo unit placeholders (public/units)</h2>
        <ul className="unit-strip">
          {DEMO_UNITS.map(({ code, label, detail }) => (
            <li key={code}>
              <img src={`/units/${code}.jpg`} alt={`Placeholder photo of unit ${label}`} width={320} height={200} />
              <span>
                <code>{label}</code> - {detail}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </main>
  )
}

export default App

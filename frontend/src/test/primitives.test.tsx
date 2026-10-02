import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  Button,
  Input,
  Card,
  Badge,
  KpiCard,
  Modal,
  Drawer,
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
  EmptyState,
  Skeleton,
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  RoleChip,
} from '../components/ui'
import { SH_COLORS } from '../tokens'

describe('Button primitive', () => {
  it('renders primary variant with primary background and white text', () => {
    render(<Button variant="primary">Confirm Booking</Button>)
    const button = screen.getByRole('button', { name: 'Confirm Booking' })
    expect(button).toBeInTheDocument()
    expect(button.className).toContain('bg-sh-primary')
    expect(button.className).toContain('text-white')
    expect(button.className).toContain('shadow-sh-button-primary')
    expect(button.className).toContain('rounded-sh-md')
    expect(button.className).toContain('h-[36px]')
    expect(button).toHaveAttribute('type', 'button')
  })

  it('renders destructive variant with error text', () => {
    render(<Button variant="destructive">Delete Unit</Button>)
    const button = screen.getByRole('button', { name: 'Delete Unit' })
    expect(button).toBeInTheDocument()
    expect(button.className).toContain('text-sh-error')
    expect(button.className).toContain('border-sh-border-strong')
  })

  it('renders secondary variant with ink-secondary text', () => {
    render(<Button variant="secondary">Cancel</Button>)
    const button = screen.getByRole('button', { name: 'Cancel' })
    expect(button.className).toContain('text-sh-ink-secondary')
    expect(button.className).toContain('border-sh-border-strong')
  })

  it('handles click events when enabled', () => {
    const handleClick = vi.fn()
    render(<Button onClick={handleClick}>Click me</Button>)
    fireEvent.click(screen.getByRole('button', { name: 'Click me' }))
    expect(handleClick).toHaveBeenCalledTimes(1)
  })

  it('disables interactions and sets aria-busy when disabled or loading', () => {
    const handleClick = vi.fn()
    const { rerender } = render(
      <Button disabled onClick={handleClick}>
        Disabled
      </Button>
    )
    const button = screen.getByRole('button', { name: 'Disabled' })
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('aria-busy', 'false')
    fireEvent.click(button)
    expect(handleClick).not.toHaveBeenCalled()

    rerender(
      <Button isLoading onClick={handleClick}>
        Save
      </Button>
    )
    const loadingBtn = screen.getByRole('button', { name: /save/i })
    expect(loadingBtn).toBeDisabled()
    expect(loadingBtn).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByText('Loading...')).toBeInTheDocument()
    fireEvent.click(loadingBtn)
    expect(handleClick).not.toHaveBeenCalled()
  })

  it('supports page-level size (40px)', () => {
    render(<Button size="page">Page CTA</Button>)
    const button = screen.getByRole('button', { name: 'Page CTA' })
    expect(button.className).toContain('h-[40px]')
  })
})

describe('Input primitive', () => {
  it('renders visible label above field in typography-label with 2px ring offset', () => {
    render(<Input label="Email address" placeholder="admin@storagehub.dev" />)
    const label = screen.getByText('Email address')
    expect(label).toBeInTheDocument()
    expect(label.className).toContain('typography-label')
    expect(label.className).toContain('text-sh-muted')

    const input = screen.getByPlaceholderText('admin@storagehub.dev')
    expect(input).toBeInTheDocument()
    expect(input.className).toContain('h-[36px]')
    expect(input.className).toContain('rounded-sh-md')
    expect(input.className).toContain('focus:ring-2')
    expect(input.className).toContain('focus:ring-sh-primary')
    expect(input.className).toContain('focus:ring-offset-1')
  })

  it('renders required indicator when required', () => {
    render(<Input label="Password" required />)
    expect(screen.getByText('*')).toBeInTheDocument()
  })

  it('renders error message in typography-meta error color and sets aria-invalid', () => {
    render(<Input label="Email" error="Invalid email address" />)
    const input = screen.getByLabelText('Email')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input.className).toContain('border-sh-error')

    const errorMessage = screen.getByRole('alert')
    expect(errorMessage).toHaveTextContent('Invalid email address')
    expect(errorMessage.className).toContain('text-sh-error')
  })

  it('renders helper text when no error is present', () => {
    render(<Input label="Code" helperText="Enter 4-digit code" />)
    expect(screen.getByText('Enter 4-digit code')).toBeInTheDocument()
  })

  it('allows typing and value change', () => {
    const handleChange = vi.fn()
    render(<Input label="Search" onChange={handleChange} />)
    const input = screen.getByLabelText('Search')
    fireEvent.change(input, { target: { value: 'S-3' } })
    expect(handleChange).toHaveBeenCalled()
  })
})

describe('Card primitive', () => {
  it('renders card with surface background, border, and rounded corners', () => {
    render(
      <Card data-testid="test-card">
        <p>Card Content</p>
      </Card>
    )
    const card = screen.getByTestId('test-card')
    expect(card.className).toContain('bg-sh-surface')
    expect(card.className).toContain('border-sh-border')
    expect(card.className).toContain('rounded-sh-md')
    expect(card.className).toContain('shadow-sh-card')
  })

  it('renders 3px left status bar with available green color (#059669)', () => {
    render(
      <Card status="available" data-testid="status-card">
        <p>Available Unit</p>
      </Card>
    )
    const bar = screen.getByTestId('card-status-bar')
    expect(bar).toBeInTheDocument()
    expect(bar.className).toContain('w-[3px]')
    expect(bar).toHaveStyle({ backgroundColor: SH_COLORS.statusAvailable })
  })

  it('renders 3px left status bar with buffer bar amber color (#F59E0B)', () => {
    render(
      <Card status="buffer" data-testid="status-card">
        <p>Buffer Unit</p>
      </Card>
    )
    const bar = screen.getByTestId('card-status-bar')
    expect(bar).toBeInTheDocument()
    expect(bar).toHaveStyle({ backgroundColor: SH_COLORS.statusBufferBar })
  })

  it('does NOT render 3px status bar when status is unknown or invalid', () => {
    render(
      <Card status="unknown-status" data-testid="unknown-card">
        <p>Unknown Unit</p>
      </Card>
    )
    expect(screen.queryByTestId('card-status-bar')).toBeNull()
  })
})

describe('Badge primitive', () => {
  it('renders 7 unit statuses with semantic colors and dot', () => {
    const { rerender } = render(<Badge status="available" />)
    expect(screen.getByText('Available')).toBeInTheDocument()
    expect(screen.getByTestId('badge-dot')).toHaveStyle({
      backgroundColor: SH_COLORS.statusAvailable,
    })

    rerender(<Badge status="buffer" />)
    expect(screen.getByText('Available soon (buffer)')).toBeInTheDocument()
    expect(screen.getByTestId('badge-dot')).toHaveStyle({
      backgroundColor: SH_COLORS.statusBufferBar,
    })

    rerender(<Badge status="reserved" />)
    expect(screen.getByText('Reserved')).toBeInTheDocument()
    expect(screen.getByTestId('badge-dot')).toHaveStyle({
      backgroundColor: SH_COLORS.statusReserved,
    })

    rerender(<Badge status="rented" />)
    expect(screen.getByText('Rented')).toBeInTheDocument()
    expect(screen.getByTestId('badge-dot')).toHaveStyle({
      backgroundColor: SH_COLORS.statusRented,
    })

    rerender(<Badge status="preparing" />)
    expect(screen.getByText('Preparing')).toBeInTheDocument()
    expect(screen.getByTestId('badge-dot')).toHaveStyle({
      backgroundColor: SH_COLORS.statusPreparing,
    })

    rerender(<Badge status="maintenance" />)
    expect(screen.getByText('Maintenance')).toBeInTheDocument()
    expect(screen.getByTestId('badge-dot')).toHaveStyle({
      backgroundColor: SH_COLORS.statusMaintenance,
    })

    rerender(<Badge status="retired" />)
    expect(screen.getByText('Retired')).toBeInTheDocument()
    expect(screen.getByTestId('badge-dot')).toHaveStyle({
      backgroundColor: SH_COLORS.statusRetired,
    })
  })

  it('renders neutral variant without crashing', () => {
    render(<Badge status="neutral">Neutral Tag</Badge>)
    expect(screen.getByText('Neutral Tag')).toBeInTheDocument()
  })

  it('preserves numeric zero as child instead of falling back to defaultLabel', () => {
    render(<Badge status="neutral">{0}</Badge>)
    expect(screen.getByText('0')).toBeInTheDocument()
  })

  it('hides dot when showDot={false}', () => {
    render(<Badge status="available" showDot={false} />)
    expect(screen.queryByTestId('badge-dot')).toBeNull()
  })
})

describe('KpiCard primitive', () => {
  it('renders label, tabular-nums value, and trend delta chip', () => {
    render(
      <KpiCard
        label="Monthly Revenue"
        value="128.500.000 ₫"
        delta="+4.2%"
        period="vs last month"
      />
    )
    expect(screen.getByText('Monthly Revenue')).toBeInTheDocument()
    expect(screen.getByText('128.500.000 ₫')).toBeInTheDocument()
    const deltaChip = screen.getByText('+4.2%')
    expect(deltaChip).toBeInTheDocument()
    expect(deltaChip.className).toContain('text-sh-status-available')
    expect(screen.getByText('vs last month')).toBeInTheDocument()
  })

  it('applies negative styling for delta starting with - or −', () => {
    const { rerender } = render(
      <KpiCard label="Churn" value="1.2%" delta="-0.8%" />
    )
    let delta = screen.getByText('-0.8%')
    expect(delta.className).toContain('text-sh-error')

    rerender(<KpiCard label="Churn" value="1.2%" delta="−1.5%" />)
    delta = screen.getByText('−1.5%')
    expect(delta.className).toContain('text-sh-error')
  })

  it('applies neutral styling when delta has no sign', () => {
    render(<KpiCard label="Occupancy" value="95%" delta="0.0%" />)
    const delta = screen.getByText('0.0%')
    expect(delta.className).toContain('text-sh-ink-secondary')
  })
})

describe('Modal primitive', () => {
  it('renders modal dialog content when open=true', () => {
    render(
      <Modal
        open={true}
        title="Payment Confirmation"
        description="Review payment details before continuing."
        footer={<Button>Pay Now</Button>}
      >
        <p>Deposit: 172.500 ₫</p>
      </Modal>
    )

    expect(screen.getByText('Payment Confirmation')).toBeInTheDocument()
    expect(screen.getByText('Review payment details before continuing.')).toBeInTheDocument()
    expect(screen.getByText('Deposit: 172.500 ₫')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Pay Now' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument()
  })

  it('does NOT render modal content when open=false', () => {
    render(
      <Modal open={false} title="Hidden Modal">
        <p>Hidden Content</p>
      </Modal>
    )
    expect(screen.queryByText('Hidden Modal')).toBeNull()
    expect(screen.queryByText('Hidden Content')).toBeNull()
  })

  it('triggers onOpenChange(false) when pressing Escape', () => {
    const handleOpenChange = vi.fn()
    render(
      <Modal open={true} onOpenChange={handleOpenChange} title="Accessible Modal">
        <p>Modal body content</p>
      </Modal>
    )

    fireEvent.keyDown(document.body, { key: 'Escape', code: 'Escape' })
    expect(handleOpenChange).toHaveBeenCalledWith(false)
  })
})

describe('Drawer primitive', () => {
  it('renders drawer right panel when open=true', () => {
    render(
      <Drawer
        open={true}
        title="Unit S-3 Details"
        description="Inspect unit configuration"
        footer={<Button variant="secondary">Dismiss</Button>}
      >
        <p>5 m2 - Preparing</p>
      </Drawer>
    )

    expect(screen.getByText('Unit S-3 Details')).toBeInTheDocument()
    expect(screen.getByText('Inspect unit configuration')).toBeInTheDocument()
    expect(screen.getByText('5 m2 - Preparing')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument()
  })

  it('does NOT render drawer content when open=false', () => {
    render(
      <Drawer open={false} title="Hidden Drawer">
        <p>Hidden Content</p>
      </Drawer>
    )
    expect(screen.queryByText('Hidden Drawer')).toBeNull()
    expect(screen.queryByText('Hidden Content')).toBeNull()
  })

  it('triggers onOpenChange(false) when pressing Escape', () => {
    const handleOpenChange = vi.fn()
    render(
      <Drawer open={true} onOpenChange={handleOpenChange} title="Accessible Drawer">
        <p>Drawer body content</p>
      </Drawer>
    )

    fireEvent.keyDown(document.body, { key: 'Escape', code: 'Escape' })
    expect(handleOpenChange).toHaveBeenCalledWith(false)
  })
})

describe('RoleChip primitive', () => {
  it('renders role chip with primary-tint background, primary-border, and role label', () => {
    render(<RoleChip role="CUSTOMER" />)
    const chip = screen.getByTestId('role-chip')
    expect(chip).toBeInTheDocument()
    expect(chip).toHaveTextContent('Customer')
    expect(chip.className).toContain('bg-sh-primary-tint')
    expect(chip.className).toContain('border-sh-primary-border')
    expect(chip.className).toContain('text-sh-primary')
    expect(chip.className).toContain('rounded-sh-sm')
  })
})

describe('Tabs primitive', () => {
  it('renders tab list and switches active tab on click', () => {
    render(
      <Tabs defaultValue="all">
        <TabsList>
          <TabsTrigger value="all">All Units</TabsTrigger>
          <TabsTrigger value="available">Available</TabsTrigger>
        </TabsList>
        <TabsContent value="all">Content for all</TabsContent>
        <TabsContent value="available">Content for available</TabsContent>
      </Tabs>
    )

    expect(screen.getByText('Content for all')).toBeInTheDocument()
    expect(screen.queryByText('Content for available')).toBeNull()

    const availableTab = screen.getByRole('tab', { name: 'Available' })
    fireEvent.mouseDown(availableTab, { button: 0 })
    fireEvent.click(availableTab)
    expect(screen.getByText('Content for available')).toBeInTheDocument()
  })
})

describe('EmptyState primitive', () => {
  it('renders title, description, filter query chips, and action CTA', () => {
    render(
      <EmptyState
        title="No units found"
        description="0 of 42 units meet all criteria"
        chips={['SIZE: 5M2', 'STATUS: AVAILABLE']}
        action={<Button variant="secondary">Reset Filters</Button>}
      />
    )

    expect(screen.getByText('No units found')).toBeInTheDocument()
    expect(screen.getByText('0 of 42 units meet all criteria')).toBeInTheDocument()
    expect(screen.getByText('SIZE: 5M2')).toBeInTheDocument()
    expect(screen.getByText('STATUS: AVAILABLE')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reset Filters' })).toBeInTheDocument()
  })
})

describe('Skeleton primitive', () => {
  it('renders skeleton with animate-pulse', () => {
    render(<Skeleton data-testid="skeleton" className="w-24 h-6" />)
    const skeleton = screen.getByTestId('skeleton')
    expect(skeleton.className).toContain('animate-pulse')
    expect(skeleton.className).toContain('rounded-[8px]')
    expect(skeleton.className).toContain('w-24')
  })
})

describe('Table primitive', () => {
  it('renders table with sticky header, 40px row, and tabular-nums numeric alignment', () => {
    render(
      <Table containerClassName="max-h-[300px]">
        <TableHeader>
          <TableRow>
            <TableHead>Unit</TableHead>
            <TableHead numeric>Monthly Rent</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell>S-3</TableCell>
            <TableCell numeric>1.150.000 ₫</TableCell>
          </TableRow>
        </TableBody>
      </Table>
    )

    const table = screen.getByRole('table')
    expect(table).toBeInTheDocument()
    const headCell = screen.getByText('Monthly Rent')
    expect(headCell.className).toContain('text-right')
    expect(headCell.className).toContain('h-[40px]')

    const numCell = screen.getByText('1.150.000 ₫')
    expect(numCell.className).toContain('text-right')
    expect(numCell.className).toContain('tabular-nums')
  })
})

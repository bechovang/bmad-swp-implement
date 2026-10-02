import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { BrowseUnitDto } from '../../types/unit'
import { Card } from '../ui/Card'
import { Badge } from '../ui/Badge'
import { Button } from '../ui/Button'
import { formatMoney } from '../../lib/format'

export interface UnitCardProps {
  unit: BrowseUnitDto
}

export function UnitCard({ unit }: UnitCardProps) {
  const navigate = useNavigate()
  const [imageError, setImageError] = useState(false)

  const handleCardClick = () => {
    navigate(`/units/${encodeURIComponent(unit.code)}`)
  }

  const handleBookClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    navigate(`/units/${encodeURIComponent(unit.code)}`)
  }

  // Determine card status for 3px left bar: buffer -> 'buffer', immediately available -> 'available'
  const cardStatus = unit.isInCleaningBuffer ? 'buffer' : 'available'

  return (
    <Card
      status={cardStatus}
      interactive
      onClick={handleCardClick}
      data-testid={`unit-card-${unit.code}`}
      className="flex flex-col h-full bg-sh-surface border border-sh-border hover:border-sh-border-strong transition-all duration-200"
    >
      {/* Thumbnail Area */}
      <div className="relative h-44 w-full bg-sh-surface-muted overflow-hidden border-b border-sh-border">
        {imageError ? (
          <div className="w-full h-full flex flex-col items-center justify-center text-sh-muted p-4 text-center">
            <span className="font-mono text-2xl font-bold tracking-wider mb-1">{unit.code}</span>
            <span className="text-xs">Image unavailable</span>
          </div>
        ) : (
          <img
            src={unit.imageUrl}
            alt={`Unit ${unit.code}`}
            onError={() => setImageError(true)}
            className="w-full h-full object-cover"
          />
        )}

        {/* Code Chip Upper-Left */}
        <div className="absolute top-2.5 left-2.5 z-10">
          <span
            data-testid="unit-code-chip"
            className="bg-sh-ink/80 text-white font-mono text-xs font-semibold px-2 py-0.5 rounded shadow-sm backdrop-blur-sm"
          >
            {unit.code}
          </span>
        </div>

        {/* Status Badge Upper-Right */}
        <div className="absolute top-2.5 right-2.5 z-10">
          <Badge
            status={unit.isInCleaningBuffer ? 'buffer' : 'available'}
            showDot
            data-testid="unit-status-badge"
          >
            {unit.isInCleaningBuffer ? 'Cleaning buffer' : 'Available'}
          </Badge>
        </div>
      </div>

      {/* Card Body */}
      <div className="p-4 flex-1 flex flex-col justify-between">
        <div className="space-y-3">
          {/* Badges line: Type and Size */}
          <div className="flex items-center gap-2">
            <Badge status="neutral" showDot={false} className="bg-sh-surface-subtle border-sh-border">
              Type {unit.typeName}
            </Badge>
            <Badge status="neutral" showDot={false} className="bg-sh-surface-subtle border-sh-border">
              {unit.sizeM2} m²
            </Badge>
          </div>

          {/* Features Line: Floor · Access · Facility */}
          <div className="text-xs text-sh-muted flex items-center gap-1.5 font-medium">
            <span>Floor {unit.floor}</span>
            <span>·</span>
            <span>{unit.accessType} Access</span>
            <span>·</span>
            <span className="truncate">{unit.facilityName || 'Tan Binh Depot'}</span>
          </div>

          {/* Availability Status Line with matching dot */}
          <div className="flex items-center gap-2 text-xs font-medium pt-1">
            <span
              className={`w-2 h-2 rounded-full shrink-0 ${
                unit.isInCleaningBuffer ? 'bg-[#F59E0B]' : 'bg-[#10B981]'
              }`}
              aria-hidden="true"
            />
            <span
              data-testid="availability-status-text"
              className={unit.isInCleaningBuffer ? 'text-[#B45309]' : 'text-[#059669]'}
            >
              {unit.availabilityStatus}
            </span>
          </div>
        </div>

        {/* Card Footer: Monthly Price + Book CTA */}
        <div className="pt-4 mt-4 border-t border-sh-divider flex items-center justify-between">
          <div>
            <span className="text-[11px] uppercase tracking-wider text-sh-muted font-semibold block">
              Monthly Rent
            </span>
            <span
              data-testid="monthly-price"
              className="text-[17px] font-bold text-sh-ink font-mono tabular-nums"
            >
              {formatMoney(unit.monthlyRate)}
              <span className="text-xs font-normal text-sh-muted ml-1">/ mo</span>
            </span>
          </div>

          <Button
            size="sm"
            variant={unit.isInCleaningBuffer ? 'secondary' : 'primary'}
            onClick={handleBookClick}
            data-testid={`book-btn-${unit.code}`}
          >
            {unit.isInCleaningBuffer ? 'Reserve' : 'Book'}
          </Button>
        </div>
      </div>
    </Card>
  )
}

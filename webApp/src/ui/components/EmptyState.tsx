/**
 * Estado vacío unificado (mensaje + CTA opcional).
 * Ver docs/UX_EMPTY_AND_ERROR_STATES.md.
 */

import { Button } from "./Button";

type EmptyStateProps = {
  title: string;
  subtitle?: string;
  ctaText?: string;
  onCtaClick?: () => void;
  className?: string;
  "aria-live"?: "polite" | "off";
};

export function EmptyState({
  title,
  subtitle,
  ctaText,
  onCtaClick,
  className = "",
  "aria-live": ariaLive = "polite",
}: EmptyStateProps) {
  return (
    <article
      className={`timeline-empty ui-empty-state ${className}`.trim()}
      aria-live={ariaLive}
    >
      <h3>{title}</h3>
      {subtitle ? <p>{subtitle}</p> : null}
      {ctaText && onCtaClick ? (
        <Button
          variant="primary"
          type="button"
          onClick={onCtaClick}
          aria-label={ctaText}
        >
          {ctaText}
        </Button>
      ) : null}
    </article>
  );
}

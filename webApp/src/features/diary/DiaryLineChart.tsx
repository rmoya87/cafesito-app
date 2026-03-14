/**
 * Gráfico de líneas del diario con Chart.js (Line Chart).
 * @see https://www.chartjs.org/docs/latest/charts/line.html
 */

import {
  CategoryScale,
  type ChartData,
  Chart as ChartJS,
  type ChartOptions,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip
} from "chart.js";
import ChartDataLabels from "chartjs-plugin-datalabels";
import { useMemo } from "react";
import { Line } from "react-chartjs-2";
import type { DiaryPeriod } from "../../core/diaryAnalytics";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
  ChartDataLabels
);

const WATER_COLOR = "#2196f3";
/** Marrón de la app: --caramel-soft / --caramel-accent (#6f4e37). */
const CAFFEINE_COLOR = "#6f4e37";

export type DiaryChartPoint = { label: string; caffeine: number; water: number };

/** Altura mínima del gráfico; usa variable CSS --chart-min-height (tokens.css). */
const CHART_MIN_HEIGHT = 'var(--chart-min-height)';

type DiaryLineChartProps = {
  chartData: DiaryChartPoint[];
  period: DiaryPeriod;
  currentSlotIndex: number;
  /** Solo en semana actual: mostrar "· Hoy" en el día actual. En semanas pasadas no se muestra. */
  isCurrentWeek?: boolean;
  /** Color café/cafeína (hex). Por defecto caramel-soft. */
  caffeineColor?: string;
};

function isDarkTheme(): boolean {
  if (typeof document === "undefined" || typeof window === "undefined") return false;
  const root = document.documentElement;
  return (
    root.classList.contains("theme-dark") ||
    (!root.classList.contains("theme-light") && window.matchMedia("(prefers-color-scheme: dark)").matches)
  );
}

export function DiaryLineChart({
  chartData,
  period,
  currentSlotIndex,
  isCurrentWeek = false,
  caffeineColor = CAFFEINE_COLOR
}: DiaryLineChartProps) {
  const isDark = isDarkTheme();
  const { data, options } = useMemo(() => {
    const labels = chartData.map((d) => d.label);
    const maxVal = Math.max(
      1,
      ...chartData.map((d) => Math.max(d.caffeine, d.water))
    );
    const datasets = [
      {
        label: "Agua (ml)",
        data: chartData.map((d) => d.water),
        borderColor: WATER_COLOR,
        backgroundColor: "transparent",
        tension: 0.35,
        fill: false,
        borderWidth: 2.5,
        pointRadius: 2.5,
        pointHoverRadius: 4,
        order: 2,
        datalabels: {
          align: "top" as const,
          anchor: "end" as const,
          color: WATER_COLOR,
          font: { size: 10, weight: "bold" as const },
          formatter: (value: number) => (value > 0 ? value : ""),
          offset: 10
        }
      },
      {
        label: "Cafeína (mg)",
        data: chartData.map((d) => d.caffeine),
        borderColor: caffeineColor,
        backgroundColor: "transparent",
        tension: 0.35,
        fill: false,
        borderWidth: 2.5,
        pointRadius: 2.5,
        pointHoverRadius: 4,
        order: 1,
        datalabels: {
          align: "top" as const,
          anchor: "end" as const,
          color: caffeineColor,
          font: { size: 10, weight: "bold" as const },
          formatter: (value: number) => (value > 0 ? value : "")
        }
      }
    ];

    const opts: ChartOptions<"line"> = {
      responsive: true,
      maintainAspectRatio: false,
      layout: {
        padding: { top: 20, left: 4, right: 4, bottom: 24 }
      },
      interaction: { intersect: false, mode: "index" },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const v = ctx.parsed.y;
              const label = ctx.dataset.label ?? "";
              if (label.startsWith("Agua")) return `${label}: ${v} ml`;
              return `${label}: ${v} mg`;
            }
          }
        },
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: {
            maxRotation: 0,
            font: (ctx: { index?: number }) => {
              const index = ctx.index ?? 0;
              const isCurrent = index === currentSlotIndex;
              return { size: 10, weight: isCurrent ? "bold" : "normal" };
            },
            color: (ctx: { index?: number }) => {
              const index = ctx.index ?? 0;
              const isCurrent = index === currentSlotIndex;
              return isCurrent ? (isDark ? "#ffffff" : "#000000") : isDark ? "#6f6760" : "#b0a8a0";
            },
            callback: function (_, index) {
              const label = labels[index];
              const isCurrent = index === currentSlotIndex;
              return isCurrent && isCurrentWeek ? `${label} · Hoy` : label;
            }
          }
        },
        y: {
          display: false,
          beginAtZero: true,
          grace: "8%",
          min: -maxVal * 0.06
        }
      }
    };

    return {
      data: { labels, datasets },
      options: opts
    };
  }, [chartData, period, currentSlotIndex, isCurrentWeek, caffeineColor, isDark]);

  return (
    <div className="diary-chartjs-wrap" style={{ minHeight: CHART_MIN_HEIGHT }}>
      <Line data={data as ChartData<"line", number[], string>} options={options} />
    </div>
  );
}

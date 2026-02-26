import { useEffect } from "react";
import { getBrewTimelineForMethod } from "../../core/brew";
import type { BrewStep } from "../../types";

type Params = {
  brewRunning: boolean;
  brewStep: BrewStep;
  brewMethod: string;
  waterMl: number;
  timerSeconds: number;
  setTimerSeconds: (updater: (prev: number) => number) => void;
  setBrewRunning: (value: boolean) => void;
  setBrewStep: (value: BrewStep) => void;
};

export function useBrewTimer({
  brewRunning,
  brewStep,
  brewMethod,
  waterMl,
  timerSeconds,
  setTimerSeconds,
  setBrewRunning,
  setBrewStep
}: Params): void {
  useEffect(() => {
    if (!brewRunning || brewStep !== "brewing") return;
    const totalDuration = getBrewTimelineForMethod(brewMethod, waterMl).reduce((acc, phase) => acc + phase.durationSeconds, 0);
    if (totalDuration > 0 && timerSeconds >= totalDuration) {
      setBrewRunning(false);
      setBrewStep("result");
      return;
    }
    const id = window.setTimeout(() => setTimerSeconds((prev) => prev + 1), 1000);
    return () => window.clearTimeout(id);
  }, [brewMethod, brewRunning, brewStep, timerSeconds, waterMl, setBrewRunning, setBrewStep, setTimerSeconds]);
}

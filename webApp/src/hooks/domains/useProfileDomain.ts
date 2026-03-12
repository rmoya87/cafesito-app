import { useCallback, useEffect, useState } from "react";
import type { TabId } from "../../types";

export function useProfileDomain(initialProfileUsername: string | null, activeTab: TabId) {
  const [profileTab, setProfileTab] = useState<"actividad" | "adn" | "favoritos">("actividad");
  const [profileUsername, setProfileUsername] = useState<string | null>(initialProfileUsername);
  const [profileEditSignal, setProfileEditSignal] = useState(0);

  useEffect(() => {
    if (activeTab === "profile") return;
    if (profileEditSignal === 0) return;
    setProfileEditSignal(0);
  }, [activeTab, profileEditSignal]);

  const triggerProfileEdit = useCallback(() => {
    setProfileEditSignal((prev) => prev + 1);
  }, []);

  return {
    profileTab,
    setProfileTab,
    profileUsername,
    setProfileUsername,
    profileEditSignal,
    setProfileEditSignal,
    triggerProfileEdit
  };
}


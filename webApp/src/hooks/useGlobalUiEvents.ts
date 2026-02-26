import { useEffect } from "react";

export function useGlobalUiEvents({
  onOpenSearch,
  onTopbarScroll
}: {
  onOpenSearch: () => void;
  onTopbarScroll: (scrolled: boolean) => void;
}) {
  useEffect(() => {
    const onShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        onOpenSearch();
      }
    };

    const onScroll = () => onTopbarScroll(window.scrollY > 18);

    window.addEventListener("keydown", onShortcut);
    window.addEventListener("scroll", onScroll, { passive: true });

    return () => {
      window.removeEventListener("keydown", onShortcut);
      window.removeEventListener("scroll", onScroll);
    };
  }, [onOpenSearch, onTopbarScroll]);
}

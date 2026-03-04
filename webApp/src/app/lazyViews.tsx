import React from "react";

/** Vistas con lazy loading para reducir el bundle inicial y mejorar LCP. */
export const LazyTimelineView = React.lazy(() =>
  import("../features/timeline/TimelineView").then((m) => ({ default: m.TimelineView }))
);
export const LazySearchView = React.lazy(() =>
  import("../features/search/SearchView").then((m) => ({ default: m.SearchView }))
);
export const LazyCoffeeDetailView = React.lazy(() =>
  import("../features/coffee/CoffeeDetailView").then((m) => ({ default: m.CoffeeDetailView }))
);
export const LazyBrewLabView = React.lazy(() =>
  import("../features/brew/BrewViews").then((m) => ({ default: m.BrewLabView }))
);
export const LazyCreateCoffeeView = React.lazy(() =>
  import("../features/brew/BrewViews").then((m) => ({ default: m.CreateCoffeeView }))
);
export const LazyDiaryView = React.lazy(() =>
  import("../features/diary/DiaryView").then((m) => ({ default: m.DiaryView }))
);
export const LazyProfileView = React.lazy(() =>
  import("../features/profile/ProfileView").then((m) => ({ default: m.ProfileView }))
);
export const LazyNotFoundView = React.lazy(() =>
  import("../features/errors/NotFoundView").then((m) => ({ default: m.NotFoundView }))
);

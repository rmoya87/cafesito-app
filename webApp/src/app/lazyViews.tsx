import React from "react";

/** Vistas con lazy loading para reducir el bundle inicial y mejorar LCP. */
export const LazyHomeView = React.lazy(() =>
  Promise.all([import("../styles/features.css"), import("../features/timeline/TimelineView")]).then(([, m]) => ({
    default: m.HomeView
  }))
);
export const LazySearchView = React.lazy(() =>
  Promise.all([import("../styles/features.css"), import("../features/search/SearchView")]).then(([, m]) => ({
    default: m.SearchView
  }))
);
export const LazyCoffeeDetailView = React.lazy(() =>
  Promise.all([import("../styles/features.css"), import("../features/coffee/CoffeeDetailView")]).then(([, m]) => ({
    default: m.CoffeeDetailView
  }))
);
export const LazyBrewLabView = React.lazy(() =>
  Promise.all([import("../styles/features.css"), import("../features/brew/BrewViews")]).then(([, m]) => ({
    default: m.BrewLabView
  }))
);
export const LazyCreateCoffeeView = React.lazy(() =>
  Promise.all([import("../styles/features.css"), import("../features/brew/BrewViews")]).then(([, m]) => ({
    default: m.CreateCoffeeView
  }))
);
export const LazyDiaryView = React.lazy(() =>
  Promise.all([import("../styles/features.css"), import("../features/diary/DiaryView")]).then(([, m]) => ({
    default: m.DiaryView
  }))
);
export const LazyProfileView = React.lazy(() =>
  Promise.all([import("../styles/features.css"), import("../features/profile/ProfileView")]).then(([, m]) => ({
    default: m.ProfileView
  }))
);
export const LazyNotFoundView = React.lazy(() =>
  Promise.all([import("../styles/features.css"), import("../features/errors/NotFoundView")]).then(([, m]) => ({
    default: m.NotFoundView
  }))
);

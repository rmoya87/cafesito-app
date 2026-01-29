import SwiftUI
import shared

struct SearchView: View {
    @StateObject private var viewModel = SharedSearchViewModel()

    var body: some View {
        VStack(spacing: 16) {
            searchBar
            if let message = viewModel.state.errorMessage {
                Text(message)
                    .foregroundStyle(.red)
            }
            if viewModel.state.coffees.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .font(.largeTitle)
                        .foregroundStyle(.secondary)
                    Text("Sin resultados")
                        .font(.headline)
                    Text("Prueba con otro término")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 32)
            } else {
                List {
                    if !viewModel.state.recentSearches.isEmpty {
                        Section("Recientes") {
                            ForEach(viewModel.state.recentSearches, id: \.self) { term in
                                Button {
                                    viewModel.onQueryChange(term)
                                } label: {
                                    Text(term)
                                }
                            }
                            .onDelete { _ in
                                viewModel.clearRecentSearches()
                            }
                        }
                    }
                    Section("Resultados") {
                        ForEach(viewModel.state.coffees, id: \.id) { coffee in
                            NavigationLink {
                                CoffeeDetailView(coffee: coffee)
                            } label: {
                                CoffeeRow(coffee: coffee)
                            }
                        }
                    }
                }
                .listStyle(.insetGrouped)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .navigationTitle("Buscar Café")
    }

    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField("Buscar por nombre u origen", text: Binding(
                get: { viewModel.state.query },
                set: { viewModel.onQueryChange($0) }
            ))
            .textInputAutocapitalization(.never)
            .disableAutocorrection(true)
            .onSubmit {
                viewModel.addSearchToHistory(viewModel.state.query)
            }
            if !viewModel.state.query.isEmpty {
                Button {
                    viewModel.onQueryChange("")
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(12)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct CoffeeRow: View {
    let coffee: Coffee

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(coffee.name)
                .font(.headline)
            Text(coffee.origin)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

private struct CoffeeDetailView: View {
    let coffee: Coffee

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(coffee.name)
                .font(.largeTitle.bold())
            Text("Origen: \(coffee.origin)")
            Text("Rating: \(String(format: "%.1f", coffee.rating))")
            Spacer()
        }
        .padding()
        .navigationTitle("Detalle")
    }
}

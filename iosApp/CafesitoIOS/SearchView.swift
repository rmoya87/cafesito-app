import SwiftUI
import Shared

struct SearchView: View {
    @StateObject private var viewModel = SearchViewModelWrapper()
    @State private var query: String = ""

    var body: some View {
        VStack(spacing: 16) {
            TextField("Buscar café", text: $query)
                .textFieldStyle(.roundedBorder)
                .onChange(of: query) { value in
                    viewModel.handle(intent: SearchIntentUpdateQuery(query: value))
                }

            HStack(spacing: 12) {
                Button("Buscar") {
                    viewModel.handle(intent: SearchIntentSubmitSearch())
                }
                .buttonStyle(.borderedProminent)

                Button("Limpiar") {
                    query = ""
                    viewModel.handle(intent: SearchIntentClearResults())
                }
                .buttonStyle(.bordered)
            }

            if viewModel.state.isLoading {
                ProgressView("Buscando...")
            } else if viewModel.state.results.isEmpty {
                Text("Sin resultados")
                    .foregroundColor(.secondary)
            } else {
                List {
                    ForEach(viewModel.state.results, id: \.id) { item in
                        SearchResultRow(item: item)
                    }
                }
            }
        }
        .padding()
        .navigationTitle("Buscar cafés")
        .alert("Error", isPresented: $viewModel.isShowingError) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(viewModel.errorMessage)
        }
    }
}

private struct SearchResultRow: View {
    let item: CoffeeSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(item.name)
                .font(.headline)
            Text("\(item.brand) • \(item.origin ?? "Origen N/A")")
                .font(.subheadline)
                .foregroundColor(.secondary)
            Text("Rating \(String(format: "%.1f", item.rating))")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

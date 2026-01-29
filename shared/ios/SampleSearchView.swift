import Combine
import SwiftUI
import shared

@MainActor
final class SearchViewModelObservable: ObservableObject {
    @Published var state: SearchUiState = SearchUiState()

    private let bridge = SharedFactory().createSearchViewModelBridge()
    private var cancellables: Set<AnyCancellable> = []

    init() {
        bridge.stateBridge.state.asPublisher()
            .receive(on: DispatchQueue.main)
            .sink { [weak self] value in
                self?.state = value
            }
            .store(in: &cancellables)

        bridge.sideEffects.asPublisher()
            .receive(on: DispatchQueue.main)
            .sink { effect in
                if let effect = effect as? SearchSideEffect.ShowMessage {
                    print("SideEffect: \(effect.message)")
                }
            }
            .store(in: &cancellables)
    }

    func onQueryChanged(_ query: String) {
        bridge.onQueryChanged(query: query)
    }

    func onSubmit() {
        bridge.onSubmit()
    }

    deinit {
        bridge.clear()
    }
}

struct SampleSearchView: View {
    @StateObject private var viewModel = SearchViewModelObservable()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            TextField("Buscar", text: Binding(
                get: { viewModel.state.query },
                set: { viewModel.onQueryChanged($0) }
            ))
            .textFieldStyle(.roundedBorder)
            .onSubmit {
                viewModel.onSubmit()
            }

            if viewModel.state.isLoading {
                ProgressView()
            } else if let error = viewModel.state.errorMessage {
                Text(error)
                    .foregroundStyle(.red)
            } else if viewModel.state.results.isEmpty {
                Text("Sin resultados")
            } else {
                List(viewModel.state.results, id: \.id) { item in
                    VStack(alignment: .leading) {
                        Text(item.title)
                        Text(item.subtitle)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .padding()
    }
}

# Paso 6 — SwiftUI bridge de ejemplo (Search)

**Estado:** vigente (referencia).  
**Última actualización:** 2026-03-04  

```swift
import Combine
import shared

final class SearchViewModelWrapper: ObservableObject {
    @Published private(set) var state: SearchUiState = SearchUiState()
    private var cancelable: AnyCancellable?
    private let viewModel: SearchViewModel

    init(viewModel: SearchViewModel) {
        self.viewModel = viewModel
        self.cancelable = FlowAdapters.observeState(viewModel.state) { [weak self] newState in
            self?.state = newState
        }
    }

    func send(_ intent: SearchIntent) {
        viewModel.handle(intent: intent)
    }

    deinit {
        cancelable?.cancel()
        viewModel.close()
    }
}

enum FlowAdapters {
    static func observeState<T>(
        _ flow: Kotlinx_coroutines_coreStateFlow,
        onEach: @escaping (T) -> Void
    ) -> AnyCancellable {
        let publisher = flow.asPublisher()
        return publisher.sink(receiveCompletion: { _ in }, receiveValue: { value in
            if let typed = value as? T {
                onEach(typed)
            }
        })
    }
}

struct SearchScreen: View {
    @StateObject var viewModel: SearchViewModelWrapper

    var body: some View {
        VStack {
            TextField("Buscar", text: Binding(
                get: { viewModel.state.filters.query },
                set: { viewModel.send(SearchIntent.UpdateQuery(query: $0)) }
            ))
            Button("Buscar") {
                viewModel.send(SearchIntent.SubmitSearch())
            }
            List(viewModel.state.results, id: \.id) { coffee in
                Text(coffee.name)
            }
        }
    }
}
```

> Nota: Este ejemplo asume un helper `asPublisher()` expuesto por el plugin KMP/Swift para flujos. Ajusta el bridge según el wrapper que uses en tu proyecto (Flow->Combine).

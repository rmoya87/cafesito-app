import Foundation
import Shared

@MainActor
final class SearchViewModelWrapper: ObservableObject {
    @Published private(set) var state: SearchUiState
    @Published private(set) var errorMessage: String = ""
    @Published var isShowingError: Bool = false

    private let viewModel: SearchViewModel
    private var stateCloseable: Closeable?
    private var effectsCloseable: Closeable?

    init() {
        let factory = SearchViewModelFactory(repository: SearchSampleRepository())
        let created = factory.create()
        self.viewModel = created
        self.state = created.state.value as! SearchUiState
        observeState()
        observeEffects()
    }

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.close()
    }

    func handle(intent: SearchIntent) {
        viewModel.handle(intent: intent)
    }

    private func observeState() {
        stateCloseable = viewModel.stateFlow().watch { [weak self] newState in
            guard let self else { return }
            self.state = newState as! SearchUiState
        }
    }

    private func observeEffects() {
        effectsCloseable = viewModel.effectsFlow().watch { [weak self] effect in
            guard let self else { return }
            if let showError = effect as? SearchEffectShowError {
                self.errorMessage = showError.message
                self.isShowingError = true
            }
        }
    }
}

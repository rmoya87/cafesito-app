import Combine
import Foundation
import shared

private final class FlowCollector<T>: Kotlinx_coroutines_coreFlowCollector {
    private let callback: (T) -> Void

    init(callback: @escaping (T) -> Void) {
        self.callback = callback
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let typedValue = value as? T {
            callback(typedValue)
        }
        completionHandler(nil)
    }
}

extension Kotlinx_coroutines_coreFlow {
    func asPublisher<T>() -> AnyPublisher<T, Never> {
        let subject = PassthroughSubject<T, Never>()
        let collector = FlowCollector<T> { value in
            subject.send(value)
        }

        collect(collector: collector) { _ in
            subject.send(completion: .finished)
        }

        return subject.eraseToAnyPublisher()
    }
}

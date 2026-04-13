
#### FBP 엔진 핵심 클래스
| 클래스 명               | 역할             | 소속 패키지          |
| --------------------- | --------------- | ----------- |
| **Node** (Component)  | 데이터를 처리하는 독립 단위 | core/         |
| **Port** (In/Out)     | 노드의 입구와 출구      | core/ |
| **Connection** (Edge) | 포트 사이를 연결하는 통로  | core/     |
| **Message** (IP)      | 노드 사이를 이동하는 데이터 | message/    |
| **Flow** (Network)    | 노드와 연결의 전체 구성도  | runner/      |
| **Runner** (Application)    | 엔진 실행 진입점 | runner/     |

> - 노드와 노드 사이에 데이터는 어떤 경로로 전달되는가?
    =>> Port를 통해 데이터가 들어오고 나가며, Connection을 통해 데이터가 흐른다
> - 노드가 동시에 동작하려면 무엇이 필요한가?
    =>> 다중 쓰레드 방식으로 구현해 프로세스가 동시 병렬적으로  수행 될 수 있게 해야한다.
> - 플로우를 "실행"한다는 것은 구체적으로 무엇을 의미하는가?
    =>> Node, Message, Port, Connection을 알맞게 배치하고 조립-실행-중지 등을 제어하는 것을 의미한다.

![fbp-engine-architecture-diagram.png](resources/fbp-engine-architecture-diagram.png)

> - ConcurrentModificationException 또는 IndexOutOfBoundsException이 발생하는가?
    =>> while 루프에서 buffer.isEmpty()를 검사하지만,
        버퍼가 비어 있지 않은 상태로 여러 소비자 쓰레드가 검사블록을 빠져나간 후, 
        버퍼에 남은 메시지 이상으로 꺼내려고 시도 할 경우... 예외가 발생한다.
        예시: 버퍼에 남은 메시지는 1개... 여러 소비자 쓰레드가 이를 확인하고 검사블록을 빠져나감... 소비자가 버퍼에 남은 메시지 수 이상으로 꺼내려고 시도.
> - 소비자가 같은 메시지를 두 번 꺼내거나, 메시지가 유실되는 경우가 있는가?
    =>> 여러 소비자가 동시에 메시지를 꺼내려고 시도하면, 메시지를 중복으로 꺼낼 수 있음
> - 소비자의 while(!buffer.isEmpty()) 루프가 CPU를 100% 점유하는가? (busy-waiting)
    =>> 멀티코어 CPU 이므로, CPU를 100% 점유 하지는 않지만 코어 한개를 단독으로 100% 점유한다.
> - 생산자가 끝난 뒤, 소비자가 종료 시점을 어떻게 알 수 있는가?
    =>> 플래그나 인터럽트를 이용 할 수 있다.

#### ArrayList, synchronized, BlockingQueue 비교
| 방식                       | 코드 라인 수         | 예외 처리 복잡도 | CPU 사용률         |
|--------------------------|-----------------|-----------|-----------------|
| ArrayList                | 가장 많음           | 복잡함       | 100% (단일 코어 전체) |
| synchronized             | 중간              | 중간        | 3.6% ~ 4.0%     |
| BlockingQueue            | 적음              | 쉬움        | 3.4% ~ 3.8%     |


### 전체 테스트 현황 점검
| 테스트 클래스 | 작성 시점 | 테스트 항목 수 | 상태 |
|-------------|----------|:----------:|--|
| `MessageTest` | Step 2 과제 2-6 | 15개 | [x] |
| `PrintNodeTest` (Step 2 기본) | Step 2 과제 2-6 | 3개 | [x] |
| `ConnectionTest` (Queue 버전) | Step 3 과제 3-11 | 4개 | [x] |
| `DefaultOutputPortTest` | Step 3 과제 3-11 | 3개 | [x] |
| `DefaultInputPortTest` | Step 3 과제 3-11 | 2개 | [x] |
| `FilterNodeTest` (Step 3 버전) | Step 3 과제 3-11 | 4개 | [x] |
| `GeneratorNodeTest` | Step 3 과제 3-11 | 4개 | [x] |
| `PrintNodeTest` (Step 3 Port 버전) | Step 3 과제 3-11 | 2개 | [x] |
| `ConnectionTest` (BlockingQueue 버전) | Step 4 과제 4-7 | 6개 | [x] |
| `AbstractNodeTest` | Step 5 과제 5-8 | 6개 | [x] |
| `TimerNodeTest` | Step 5 과제 5-8 | 4개 | [x] |
| `PrintNodeTest` (Step 5 리팩토링 후) | Step 5 과제 5-8 | 3개 | [x] |
| `FilterNodeTest` (리팩토링 후) | Step 5 과제 5-8 | 3개 | [x] |
| `LogNodeTest` | Step 5 과제 5-8 | 2개 | [x] |
| `TransformNodeTest` | Step 6 과제 6-8 | 3개 | [x] |
| `SplitNodeTest` | Step 6 과제 6-8 | 4개 | [x] |
| `CounterNodeTest` | Step 6 과제 6-8 | 3개 | [x] |
| `DelayNodeTest` | Step 6 과제 6-8 | 2개 | [x] |
| `FlowTest` | Step 7 과제 7-6 | 12개 | [x] |
| `FlowEngineTest` | Step 8 과제 8-6 | 9개 | [x] |
| `TemperatureSensorNodeTest` | Step 9 과제 9-8 | 4개 | [x] |
| `ThresholdFilterNodeTest` | Step 9 과제 9-8 | 5개 | [x] |
| `AlertNodeTest` | Step 9 과제 9-8 | 2개 | [x] |
| `FileWriterNodeTest` | Step 9 과제 9-8 | 3개 | [x] |
| `HumiditySensorNodeTest` | Step 9 과제 9-8 | 4개 | [x] |
| `MergeNodeTest` | Step 9 과제 9-8 | 4개 | [x] |
| 온도 모니터링 통합 테스트 | Step 9 과제 9-8 | 3개 | [x] |
| `CollectorNodeTest` | Step 10 과제 10-1 | 5개 | [x] |
| 최종 종합 통합 테스트 | Step 10 과제 10-6 | 7개 | [x] |
| **합계** | | **약 125개** |  |
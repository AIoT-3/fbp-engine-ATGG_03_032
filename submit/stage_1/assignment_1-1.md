
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
# GLAP1

GLAP1은 [Paper](https://github.com/PaperMC/Paper)를 바탕으로 만든 우리만의 마인크래프트 서버 구동기예요.
Paper 플러그인을 그대로 쓸 수 있고, GLAP만의 설정과 명령어가 더해져 있어요.

- Minecraft: **26.3**
- Java: **25 이상**
- 바탕: Paper `5050b0f33` (`gradle.properties`의 `paperCommit`)

## 기능

| 기능 | 설명 |
|---|---|
| GLAP1 브랜드 | `/version`과 콘솔에 서버 이름이 GLAP1으로 표시돼요. |
| `glap.yml` | 게임 규칙, 업데이트 확인, Paper 내장 Metrics를 설정해요. 서버 폴더에 자동으로 만들어져요. |
| `/glap` | `/glap version`으로 버전을 확인하고, `/glap status`로 설정 상태를 보고, `/glap reload`로 설정을 다시 읽어요. OP만 쓸 수 있어요. |
| 1.12.2 명령어 호환 | `/blockdata`, `/entitydata`, `/xp`, `/effect`, `/replaceitem`의 지원되는 이름 기반 문법을 현재 명령어로 변환해요. |
| 업데이트 알림 | 비공개 GitHub 저장소를 확인하려면 서버 프로세스에 `GLAP_GITHUB_TOKEN`을 설정하세요. 토큰에는 저장소 Contents 읽기 권한이 필요해요. 미설정 시 원격 확인은 기본으로 꺼져요. `updates.enabled: false`로 수동 확인도 끌 수 있어요. |
| Kode 런타임 | Kode가 GLAP 서버 jar에 내장되고, 시작할 때 `plugins/Kode.jar`로 자동 설치돼요. Kotlin 플러그인은 `depend: [Kode]`로 공용 런타임을 사용해요. |

### 1.12.2 명령어 호환

**목표는 Java Edition 1.12.2의 바닐라 명령어 전체를 호환하는 것**이에요. 여기에는 명령어 이름과 별칭, 예전 인자 문법, 선택자, 좌표, 숫자형 ID, 메타데이터, SNBT, 실행 결과와 권한 동작이 포함돼요. 이 작업은 최신 클라이언트로 접속해 1.12.2 명령어를 입력하는 호환 기능이며, 1.12.2 클라이언트 프로토콜 접속을 제공하지는 않아요.

번역은 명령어 전처리 이벤트 이후, 현재 명령어 처리기로 실행하기 전에 적용돼요. 플러그인 이벤트, 권한 검사, 현재 버전의 유효성 검사는 그대로 동작해요.

#### 2026-11-06 작업 요약

- 명령 실행 경계에 레거시 명령어 번역기를 연결했어요. 번역한 명령도 현재 명령어 처리기에서 실행돼요.
- 다음 명령어의 아래에 적힌 형식을 현재 문법으로 바꾸는 기능을 추가했어요.
  - `/blockdata <x> <y> <z> <snbt>` → `/data merge block ...`
  - `/entitydata <대상> <snbt>` → `/data merge entity ...`
  - `/xp <양>[L] [대상]` → 경험치 포인트 또는 레벨 추가
  - `/effect <대상> clear`와 이름 기반 효과 부여 문법. `0`초는 제거, `-1`초는 무기한 지속으로 변환
  - `/replaceitem`의 개체·블록 대상 문법과 `slot.*` 슬롯 이름
- 위 명령의 지원 형식은 격리 서버에서 실행 확인했고, 빌드와 릴리스 검증을 마쳤어요. [v3.1.0 릴리스](https://github.com/winterliy/GLAP/releases/tag/v3.1.0)

`/xp`, `/effect`, `/replaceitem`은 일부 문법만 지원해요. 콘솔 실행, 숫자형 ID, 아이템 내구도 메타데이터, 구형 아이템 NBT 등 아래 미완료 항목은 아직 호환되지 않을 수 있어요.

#### 미완료 명령어와 호환 작업

아래 목록은 Java Edition 1.12.2의 바닐라 명령어 기준이에요. `일부 구현`은 README 위의 지원 형식만 동작한다는 뜻이에요. `호환 작업 필요`는 현재 버전에 같은 이름의 명령이 있더라도 1.12.2 문법과 동작을 확인·보완해야 한다는 뜻이에요. 명령어 별칭도 함께 적었어요.

| 상태 | 1.12.2 명령어 |
|---|---|
| 지원 형식 있음, 나머지 호환 작업 필요 | `/blockdata`, `/entitydata`, `/effect`, `/replaceitem`, `/xp` |
| 호환 작업 필요 | `/advancement`, `/ban`, `/ban-ip`, `/banlist`, `/clear`, `/clone`, `/debug`, `/defaultgamemode`, `/deop`, `/difficulty`, `/enchant`, `/execute`, `/fill`, `/function`, `/gamemode`, `/gamerule`, `/give`, `/help` (`/?`), `/kick`, `/kill`, `/list`, `/locate`, `/me`, `/msg` (`/tell`, `/w`), `/op`, `/pardon`, `/pardon-ip`, `/particle`, `/playsound`, `/publish` (싱글플레이/LAN), `/recipe`, `/reload`, `/save-all`, `/save-off`, `/save-on`, `/say`, `/scoreboard`, `/seed`, `/setblock`, `/setidletimeout`, `/setworldspawn`, `/spawnpoint`, `/spreadplayers`, `/stats`, `/stop`, `/stopsound`, `/summon`, `/tellraw`, `/testfor`, `/testforblock`, `/testforblocks`, `/time`, `/title`, `/toggledownfall`, `/tp` (`/teleport`), `/trigger`, `/weather`, `/whitelist`, `/worldborder` |

`/achievement`은 1.12에서 advancement로 대체되어 1.12.2 명령 목록에 포함하지 않았어요. Bukkit/Paper 플러그인이 추가하는 명령어는 바닐라 기준 목록에 포함되지 않아요.

#### 남은 구현 순서

- [ ] `/execute`: 기본 실행 형식과 `detect`의 일부 구형 블록 ID·데이터 값 변환을 구현했어요. 와일드카드는 여러 블록과 상태 속성(예: `stone -1`, `log -1`)을 검사하며 절대·상대 소수 좌표를 섞어 쓸 수 있어요. 나머지 구형 선택자, 중첩 실행 및 함수·명령 블록 경로의 호환과 검증이 남아 있어요.

1. 위 명령어와 별칭 각각에 대해 1.12.2의 사용 형식, 선택자, 권한, 콘솔·플레이어·명령 블록·함수 실행 차이를 기준으로 회귀 사례를 정리해요.
2. `/execute`, `/testfor*`, `/stats`, `/scoreboard`처럼 삭제되거나 구조가 크게 바뀐 문법을 우선 호환해요.
3. 아이템·블록·효과·인챈트의 숫자 ID와 데이터 값, 아이템 메타데이터 및 SNBT를 현재 레지스트리 형식으로 변환해요.
4. 나머지 명령어의 인자 변경, 선택자 인자, 별칭, 권한 및 실행 결과를 보완해요.
5. 모든 명령어를 플레이어, 콘솔, 명령 블록, 함수에서 검증하고, 완료한 항목을 이 목록에서 갱신해요.

명령 목록은 [Minecraft Wiki의 명령어 문서](https://minecraft.wiki/w/Commands)와 [Java Edition 1.12 업데이트 문서](https://minecraft.wiki/w/Java_Edition_1.12)를 기준으로 정리했어요.

### `glap.yml`

```yaml
config-version: 1

gameplay:
  # false면 엔더맨이 블록을 집어 가지 않아요.
  enderman-picks-up-blocks: true
  # false면 크리퍼가 터져도 블록은 부서지지 않아요. 주변 생명체는 그대로 피해를 입어요.
  creeper-destroys-blocks: true
  # false면 플레이어와 몹이 밭을 밟아도 흙으로 변하지 않아요.
  farmland-trampling: true
  # true면 엔드에서도 물을 설치할 수 있어요.
  allow-water-placement-in-end: false
  # true면 디스펜서가 모루를 블록으로 설치해요.
  dispensers-place-anvils: false
  # true면 활에 무한과 수선을 함께 부여할 수 있어요.
  allow-infinity-and-mending-together: false

updates:
  # false면 시작 시 자동 확인과 /glap version의 원격 확인을 끄고 로컬 버전만 표시해요.
  enabled: true

privacy:
  # Paper 내장 Metrics만 제어해요. 변경 사항은 재시작 후 적용돼요.
  paper-metrics-enabled: true
```

설정 파일은 `config-version`에 따라 관리돼요. 이전 버전 파일을 갱신할 때는 기존 파일을 `.bak`으로 백업하고, 잘못된 YAML이나 지원하지 않는 미래 버전은 현재 실행 설정에 적용하지 않아요. 새 게임플레이 옵션은 모두 기본값에서 Paper/바닐라 동작을 유지해요.

`privacy.paper-metrics-enabled`는 Paper의 기본 Metrics만 제어하며 Spark, JMX, 운영체제 통계, 플러그인 자체 Metrics는 제어하지 않아요. `/glap status`에서 설정값과 현재 실행 중인 상태를 볼 수 있고, 재시작이 필요한 변경도 알려줘요. GLAP1은 Paper 플러그인 호환성을 유지하며 Folia 지원이나 지역별 병렬 틱을 제공하지 않아요.

### GLAP 플러그인 API

가짜 엔티티 API와 Bukkit 상태·패킷 전송은 서버 메인 스레드에서 사용해야 해요. 비동기 작업이 끝난 뒤에는 GLAP 스레드 경계를 통해 메인 스레드로 돌아와요.

```java
GlapApi.get().serverThread().execute(plugin, () -> {
    FakeEntity<ArmorStand> display = GlapApi.get().fakeEntities().create(location, ArmorStand.class);
    display.updateMetadata(stand -> {
        stand.setInvisible(true);
        stand.setCustomName(Component.text("GLAP"));
        stand.setCustomNameVisible(true);
    });
    display.spawnTo(player);
});
```

가짜 엔티티는 실제 월드 엔티티 목록에 들어가지 않아요. `spawnTo`로 지정한 플레이어에게만 보이고, `teleport`, `updateMetadata`, `updateEquipment`, `hideFrom`, `remove`도 같은 메인 스레드 규칙을 따라요. 가짜 플레이어 엔티티는 현재 지원하지 않아요.

## 서버 실행

1. [릴리스](https://github.com/winterliy/GLAP/releases)에서 `glap-<버전>.jar`를 받아 서버 폴더에 넣어요.
2. GLAP 서버 jar를 실행해요. Kode는 `plugins/`에 자동 설치돼 Kotlin 플러그인이 시작할 때 사용할 수 있어요.

   ```sh
   java -Xms3G -Xmx3G -jar glap-<버전>.jar --nogui
   ```

처음 실행할 때 Mojang 서버 파일을 내려받아 패치해요. 그래서 jar 안에는 Mojang 코드가 들어 있지 않아요.

## 소스에서 빌드

```sh
git clone https://github.com/winterliy/GLAP.git
cd GLAP
./gradlew applyAllPatches      # Paper 코드를 받아 GLAP 패치를 적용
./gradlew createPaperclipJar   # Kode를 내장한 서버 jar 생성
```

Kode 플러그인 파일은 GLAP 서버 jar 안에 내장돼요. 서버 시작 때 기존 `plugins/Kode.jar`가 없으면 설치하고, GLAP이 설치한 파일은 서버 업그레이드 때 자동 갱신해요. 사용자가 별도로 설치한 Kode 파일이나 GLAP 설치 후 직접 바꾼 파일은 덮어쓰지 않아요. Kode의 소스, 버전, 빌드는 GLAP 저장소에서 관리해요.

zip으로 받으면 빌드되지 않아요. 반드시 `git clone`으로 받아주세요.

## 구조

[Purpur](https://github.com/PurpurMC/Purpur)와 같은 방식이에요. Paper 코드를 저장소에 복사하지 않고, Paper의 특정 커밋 위에 GLAP 패치만 얹어요.

```
gradle.properties                 paperCommit(바탕 Paper 커밋), glapVersion(GLAP 버전)
glap-api/                         API 쪽 패치
glap-server/
├── src/main/java/dev/glap/       Paper와 맞닿는 GLAP 코드 (Java)
├── src/main/kotlin/dev/glap/     GLAP 내부 로직 (Kotlin)
├── paper-patches/                Paper 코드에 얹는 패치 (Java)
└── minecraft-patches/            마인크래프트 코드에 얹는 패치 (Java)
kode/                             GLAP 서버에 내장할 Kotlin 런타임 제공 플러그인 소스
paper-api/, paper-server/         applyAllPatches가 생성 (Git에 올라가지 않음)
```

- Paper·마인크래프트 코드가 직접 부르는 클래스(`GlapConfig`, `GlapCommand`, `GlapVersionFetcher`)는 `src/main/java`에 Java로 써요.
- Paper와 상관없는 GLAP 내부 로직(예: `GitHubUpdateChecker` GitHub 릴리스 확인)은 `src/main/kotlin`에 Kotlin으로 써요.
- Java에서 Kotlin 클래스를 직접 부르지 않아요. Java 인터페이스(`UpdateChecker`)를 Kotlin이 구현하고 `META-INF/services`로 불러와요. 그래야 VS Code(Eclipse 컴파일러)에서도 오류가 안 나요.
- 패치에는 위 클래스를 부르는 한두 줄만 넣어요. 패치가 작을수록 Paper 업데이트가 쉬워요.
- Paper·마인크래프트 코드에 넣는 패치는 원본이 Java라 Java로 써요. 바꾼 줄에는 `// GLAP` 주석을 달아요.

### 패치 수정하기

1. `./gradlew applyAllPatches`
2. `glap-server/src/minecraft/java`(마인크래프트) 또는 `paper-server/`(Paper)에서 코드를 고쳐요.
3. 고친 내용을 패치 파일로 저장해요.

   ```sh
   ./gradlew fixupMinecraftSourcePatches fixupPaperServerFilePatches
   ./gradlew rebuildAllServerFilePatches
   ```

## Paper 업데이트

GitHub Actions가 매주 월요일 Paper에 새 커밋이 있는지 확인하고, 있으면 이슈를 열어요.

1. `gradle.properties`의 `paperCommit`을 새 Paper 커밋으로 바꿔요.
2. `./gradlew applyAllPatches`를 실행해요. 패치 충돌이 나면 고친 뒤 패치를 다시 저장해요.
3. 빌드해서 서버를 테스트하고 새 버전으로 릴리스해요.

Minecraft 버전이 바뀌면(예: 26.4) 새 브랜치(`26.4`)를 만들어요. 브랜치 이름은 Minecraft 버전과 같아요.

## 릴리스

버전은 `MAJOR.MINOR.PATCH`이고, 변경 내용은 [CHANGELOG.md](CHANGELOG.md)에 적어요.

1. `gradle.properties`의 `glapVersion`과 `CHANGELOG.md`를 고쳐 커밋해요.
2. `v<버전>` 태그를 push하면 GitHub Actions가 jar를 빌드해 릴리스를 만들어요.

   ```sh
   git tag -a v3.0.0 -m "GLAP1 3.0.0"
   git push origin v3.0.0
   ```

## 라이선스

GLAP1은 Paper와 같은 라이선스를 따라요. 자세한 내용은 [LICENSE.md](LICENSE.md)를 보세요.
Paper가 바탕으로 삼은 CraftBukkit 코드는 GPLv3예요. 그래서 GLAP1 jar를 다른 사람에게 배포하면 소스 코드도 함께 공개해야 해요.

GLAP1은 [PaperMC](https://papermc.io/)와 [Purpur](https://purpurmc.org/) 덕분에 만들 수 있었어요.

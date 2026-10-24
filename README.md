# GLAP1

GLAP1은 [Paper](https://github.com/PaperMC/Paper)를 바탕으로 만든 우리만의 마인크래프트 서버 구동기예요.
Paper 플러그인을 그대로 쓸 수 있고, GLAP만의 설정과 명령어가 더해져 있어요.

- Minecraft: **26.3**
- Java: **25 이상**
- 바탕: Paper `1b9bf5c8e` (`gradle.properties`의 `paperCommit`)

## 기능

| 기능 | 설명 |
|---|---|
| GLAP1 브랜드 | `/version`과 콘솔에 서버 이름이 GLAP1으로 표시돼요. |
| `glap.yml` | 게임 규칙을 설정 파일로 켜고 꺼요. 서버 폴더에 자동으로 만들어져요. |
| `/glap` | `/glap version`으로 업데이트를 확인하고, `/glap reload`로 `glap.yml`을 다시 불러와요. OP만 쓸 수 있어요. |
| 업데이트 알림 | 서버가 켜질 때 GitHub 릴리스를 확인해서 새 버전이 있으면 알려줘요. |

### `glap.yml`

```yaml
gameplay:
  # false면 엔더맨이 블록을 집어 가지 않아요.
  enderman-picks-up-blocks: true
  # false면 크리퍼가 터져도 블록은 부서지지 않아요. 주변 생명체는 그대로 피해를 입어요.
  creeper-destroys-blocks: true
  # false면 플레이어와 몹이 밭을 밟아도 흙으로 변하지 않아요.
  farmland-trampling: true
```

## 서버 실행

1. [릴리스](https://github.com/winterliy/GLAP/releases)에서 `glap-<버전>.jar`를 받아요.
2. 서버 폴더에 넣고 실행해요.

   ```sh
   java -Xms3G -Xmx3G -jar glap-<버전>.jar --nogui
   ```

처음 실행할 때 Mojang 서버 파일을 내려받아 패치해요. 그래서 jar 안에는 Mojang 코드가 들어 있지 않아요.

## 소스에서 빌드

```sh
git clone https://github.com/winterliy/GLAP.git
cd GLAP
./gradlew applyAllPatches      # Paper 코드를 받아 GLAP 패치를 적용
./gradlew createPaperclipJar   # glap-server/build/libs/ 에 jar 생성
```

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
paper-api/, paper-server/         applyAllPatches가 생성 (Git에 올라가지 않음)
```

- Paper·마인크래프트 코드가 직접 부르는 클래스(`GlapConfig`, `GlapCommand`, `GlapVersionFetcher`)는 `src/main/java`에 Java로 써요.
- Paper와 상관없는 GLAP 내부 로직(예: `GlapReleases` GitHub 릴리스 확인)은 `src/main/kotlin`에 Kotlin으로 써요.
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
   git tag -a v1.1.0 -m "GLAP1 1.1.0"
   git push origin v1.1.0
   ```

## 라이선스

GLAP1은 Paper와 같은 라이선스를 따라요. 자세한 내용은 [LICENSE.md](LICENSE.md)를 보세요.
Paper가 바탕으로 삼은 CraftBukkit 코드는 GPLv3예요. 그래서 GLAP1 jar를 다른 사람에게 배포하면 소스 코드도 함께 공개해야 해요.

GLAP1은 [PaperMC](https://papermc.io/)와 [Purpur](https://purpurmc.org/) 덕분에 만들 수 있었어요.

# Place Mock Data Generator

## 목적

이 도구는 성능 테스트용 "place" 데이터만 생성하고 적재한다. "drama",
"scene", "member", "wishlist", "travel_plan", "place_img" 데이터는 생성하지
않는다.

기준 스키마는 backend/src/test/resources/init.sql의 "place"와
"content_type"이다.

적재 컬럼은 다음 순서로 고정되어 있다.

    content_id, content_type_id, name, address, latitude, longitude,
    description, src_created_at, src_updated_at

"place_id"는 AUTO_INCREMENT에 맡기고 "created_at", "updated_at"은 MySQL
기본값에 맡긴다. "description"은 빈 CSV 값을 NULL로 변환한다.

`place.content_id`는 `init.sql`과 Place DTO가 모두 `INT` 계약을 사용한다.
Mock 데이터는 이 타입을 지키면서도 기존 원천 콘텐츠 번호와 충돌하지 않도록
설정된 높은 숫자 범위를 seed별 namespace로 나누어 사용한다. 숫자 범위가
이미 사용 중이면 Generator가 적재를 중단한다.

## 분석 결과

- 현재 Place 조회는 JPA Repository가 아니라 MyBatis PlaceMapper.xml을
  사용한다.
- 검색은 "name", "address", "description"의 %keyword% 부분 검색,
  "content_type_id" equality, 위도/경도 차이의 사각 범위 조건을 사용한다.
- 근처 장소 조회는 "content_type_id"와 위도/경도 사각 범위를 사용하고
  거리 계산 후 정렬한다.
- "place"에는 PRIMARY KEY와 "content_id" UNIQUE만 있고 카테고리/좌표/검색
  전용 인덱스는 현재 스키마에 없다. Generator는 인덱스를 임의로 추가하지
  않고 현재 조회 방식에 의미 있는 데이터 분포를 만든다.

## content_type_id

init.sql의 정적 기준 행을 그대로 설정에 반영했다.

    12 관광지
    14 문화시설
    15 축제공연행사
    25 여행코스
    28 레포츠
    32 숙박
    38 쇼핑
    39 관광지

각 Place CSV 행에는 위 ID 중 하나가 반드시 들어간다. "content_type" 테이블
자체를 Mock Data로 대량 생성하지는 않는다. 적재 전 FK와 ID/이름을 검증하며,
기준 행이 없거나 이름이 다르면 적재를 중단한다.

## 데이터 분포

- 기본 seed는 3671이며, 행별 seed를 계산하므로 chunk를 나누거나 재생성해도
  같은 설정의 같은 행이 생성된다.
- 기본 profile은 small=10,000, medium=500,000, real=6,000,000이다.
- 기본 chunk는 500,000건이다.
- 수도권 52%, 부산/울산 12%, 대구/경북 9%, 대전/충청 10%,
  광주/전라 8%, 강원 5%, 제주 4% 가중치를 사용한다.
- 각 지역에는 여러 hotspot 중심과 Gaussian 표준편차가 설정되어 있다.
  수도권 상권은 좁은 표준편차와 많은 행으로, 지방 및 저밀도 hotspot은
  넓은 표준편차와 적은 행으로 만든다.
- 34개 hotspot에 약 201개의 가중 주소 후보를 두었다. 선택한 hotspot의
  좌표 밀도는 유지하면서 시/군/구/읍/면/동 후보를 분산해 주소 검색어의
  결과 건수 차이를 만들고, 선택된 주소 후보와 좌표가 같은 도시권에
  있도록 구성한다.
- "공원"은 자주, "문화"는 중간 빈도, "별빛누리"는 희귀 빈도로 이름에
  등장하도록 구성했다.
- description은 NULL/짧은 값부터 1,400자 값까지 여러 bucket으로 나누며,
  TEXT 최대 바이트를 넘지 않도록 60,000 bytes에서 제한한다.
- src_created_at <= src_updated_at를 보장하고 날짜를 2016~2026 기간에
  분산한다.
- content_id는 백엔드의 int 계약에 맞춘 숫자형 deterministic namespace를
  사용한다. 기본 seed 3671의 범위는 1,838,000,000~1,843,999,999이며,
  같은 숫자 namespace에 기존 행이 있으면 기본 동작은 중단한다.

## 실행

Python 표준 라이브러리만 사용한다. MySQL CLI가 PATH에 없으면
--mysql-bin으로 실행 파일 경로를 지정한다.

## 홈서버에서 Python 파일 직접 실행

현재 운영 방식은 GitHub Actions 이미지가 아니라 Python 파일을 홈서버에
직접 업로드해 실행하는 방식이다. 홈서버의 `compose.yml`은 MySQL과
애플리케이션을 기동하는 운영 파일이고, 이 저장소의
`docker-compose.pilgrimage.yml`을 홈서버에 복사할 필요는 없다.

### 1. 홈서버에 필요한 파일 업로드

generator, 설정 파일, 새 스키마를 홈서버에 복사한다. `validate_place_mock.sql`은
수동 검증을 할 때만 필요하다.

    mkdir -p /opt/pilgrimage/mock_data /data/place-mock

개발 PC에서 실행하는 예시:

    scp mock_data/generate_place_mock.py user@home-server:/opt/pilgrimage/mock_data/
    scp mock_data/place_mock_config.json user@home-server:/opt/pilgrimage/mock_data/
    scp mock_data/validate_place_mock.sql user@home-server:/opt/pilgrimage/mock_data/
    scp backend/src/test/resources/init.sql user@home-server:/opt/pilgrimage/mock_data/init.sql

실제 업로드 경로는 홈서버 정책에 맞게 바꾼다. Python 파일과 JSON은 같은
버전으로 올려야 하며, 설정을 바꾸지 않았다면 `seed`는 3671이다.

### 2. 홈서버 실행 환경 준비

Ubuntu/Debian 예시는 다음과 같다. Python 3.10 이상과 MySQL CLI만 필요하며
별도 pip 패키지는 필요하지 않다.

    sudo apt-get update
    sudo apt-get install -y python3 default-mysql-client
    python3 --version
    mysql --version

홈서버의 compose 파일 위치와 서비스명을 확인한다. 아래에서는 파일이
`/opt/pilgrimage/compose.yml`, MySQL 서비스명이 `mysql`이라고 가정한다.
실제 서비스명이 `db` 등으로 다르면 명령의 `mysql`을 바꾼다.

    cd /opt/pilgrimage
    docker compose -f compose.yml config --services
    docker compose -f compose.yml ps

Python을 홈서버 호스트에서 실행하므로 MySQL 포트가 호스트에 publish되어
있어야 한다. 예를 들어 compose의 MySQL 서비스에 다음과 같은 매핑이
있으면 generator의 접속 host는 `127.0.0.1`이다.

    ports:
      - "127.0.0.1:3306:3306"

포트가 publish되지 않았다면 홈서버의 실제 MySQL 접속 주소와 포트를
사용하거나, 먼저 운영 compose에 안전한 loopback 포트 매핑을 추가한다.

### 3. LOAD DATA LOCAL INFILE 설정 확인

이 도구는 MySQL CLI의 `LOAD DATA LOCAL INFILE`을 사용한다. MySQL 서버의
`local_infile`도 켜져 있어야 한다. 홈서버 `compose.yml`의 MySQL 서비스에
다음 옵션을 추가한다. 기존 `command`가 있으면 덮어쓰지 말고 옵션을
합쳐야 한다.

    services:
      mysql:
        command:
          - --local-infile=1

compose 파일을 수정했다면 MySQL 컨테이너만 설정을 재생성한다.

    docker compose -f compose.yml up -d --force-recreate mysql
    docker compose -f compose.yml exec mysql \
      mysql -uroot -p -Nse "SELECT @@local_infile;"

결과가 `1`이어야 한다. `docker compose down -v`는 기존 DB 볼륨을 삭제할
수 있으므로 사용하지 않는다. generator 자체도 MySQL CLI 실행 시
`--local-infile=1`을 자동으로 전달한다.

### 4. 대상 DB와 비밀번호 파일 준비

기존 운영 DB에 `init.sql`을 바로 실행하지 않는다. `init.sql`에는 테이블
삭제 구문이 포함될 수 있으므로 기존 데이터가 삭제될 수 있다. `place`와
`content_type`이 이미 존재하는 DB라면 generator의 `run --dry-run`이 실제
스키마, FK, content type 기준값을 검증한다.

비밀번호는 홈서버의 기존 비밀 환경 파일을 사용한다. 예시는
`/opt/pilgrimage/.env`이며 실제 경로로 변경한다.

    chmod 600 /opt/pilgrimage/.env

이 파일에 `SPRING_DATASOURCE_USERNAME`과
`SPRING_DATASOURCE_PASSWORD`가 있으면 아래 명령에서 `--user`를 생략할 수
있다. 명시적인 DB 이름은 반드시 `--database`로 전달한다.

### 5. dry-run

홈서버에서 실행한다. `--env-file`은 홈서버에 있는 실제 파일 경로다.

    python3 /opt/pilgrimage/mock_data/generate_place_mock.py \
      --config /opt/pilgrimage/mock_data/place_mock_config.json \
      run \
      --profile small \
      --database moonbackdb \
      --host 127.0.0.1 \
      --port 3306 \
      --user YOUR_USER \
      --password-env SPRING_DATASOURCE_PASSWORD \
      --env-file /opt/pilgrimage/.env \
      --output-dir /data/place-mock \
      --dry-run

실제 row는 적재하지 않으며 대상 DB/테이블, 컬럼 타입, NOT NULL/UNIQUE,
content_type FK와 기준값, `local_infile`, 동일 숫자 namespace의 기존 건수를
확인한다.

### 6. small profile 적재와 검증

처음에는 10,000건을 chunk 1,000건으로 적재한다.

    python3 /opt/pilgrimage/mock_data/generate_place_mock.py \
      --config /opt/pilgrimage/mock_data/place_mock_config.json \
      run \
      --profile small \
      --chunk-size 1000 \
      --database moonbackdb \
      --host 127.0.0.1 \
      --port 3306 \
      --user YOUR_USER \
      --password-env SPRING_DATASOURCE_PASSWORD \
      --env-file /opt/pilgrimage/.env \
      --output-dir /data/place-mock

적재 후 자동 검증을 실행한다.

    python3 /opt/pilgrimage/mock_data/generate_place_mock.py \
      --config /opt/pilgrimage/mock_data/place_mock_config.json \
      verify \
      --profile small \
      --database moonbackdb \
      --host 127.0.0.1 \
      --port 3306 \
      --user YOUR_USER \
      --password-env SPRING_DATASOURCE_PASSWORD \
      --env-file /opt/pilgrimage/.env

### 7. real profile 적재

small 결과와 디스크 여유 공간을 확인한 뒤 6,000,000건을 실행한다.

    python3 /opt/pilgrimage/mock_data/generate_place_mock.py \
      --config /opt/pilgrimage/mock_data/place_mock_config.json \
      run \
      --profile real \
      --chunk-size 500000 \
      --database moonbackdb \
      --host 127.0.0.1 \
      --port 3306 \
      --user YOUR_USER \
      --password-env SPRING_DATASOURCE_PASSWORD \
      --env-file /opt/pilgrimage/.env \
      --output-dir /data/place-mock

### 8. 실패 후 재개

실패 후에는 같은 Python 파일, JSON 설정, seed, profile, count,
chunk-size, output 디렉터리를 유지하고 `--resume`을 붙인다.

    python3 /opt/pilgrimage/mock_data/generate_place_mock.py \
      --config /opt/pilgrimage/mock_data/place_mock_config.json \
      run \
      --profile real \
      --database moonbackdb \
      --host 127.0.0.1 \
      --port 3306 \
      --user YOUR_USER \
      --password-env SPRING_DATASOURCE_PASSWORD \
      --env-file /opt/pilgrimage/.env \
      --output-dir /data/place-mock \
      --resume

manifest는 `/data/place-mock`에 남아 있어야 한다. 이미 정확히 적재된
chunk는 건너뛰고, 일부만 적재된 chunk는 자동 삭제하지 않고 실패시켜
운영 데이터를 덮어쓰지 않는다. 성공한 chunk CSV는 기본적으로 삭제되고
manifest만 남는다.

Python generator가 호스트에서 실행되므로 MySQL host에는 Docker 서비스명
`mysql`을 쓰지 않는다. `mysql` 서비스명은 같은 Docker 네트워크에 붙은
컨테이너에서 실행할 때만 사용할 수 있다. 이 작업에서는 호스트에 publish된
주소인 `127.0.0.1`과 포트를 사용한다.

## CSV만 별도 생성

DB에 바로 적재하지 않고 CSV/manifest만 생성하려면 다음처럼 실행한다.

    python3 /opt/pilgrimage/mock_data/generate_place_mock.py \
      --config /opt/pilgrimage/mock_data/place_mock_config.json \
      generate \
      --profile small \
      --chunk-size 1000 \
      --output-dir /data/place-mock

`run`은 CSV 생성과 bulk load를 chunk별로 수행하고, `load`는 이미 생성된
manifest의 CSV를 적재한다. 모든 DB 명령은 `--database`를 명시해야 한다.

## bulk load 방식

CSV를 한 chunk씩 생성한 뒤 MySQL CLI의 LOAD DATA LOCAL INFILE로 적재한다.
성공한 chunk는 기본적으로 즉시 삭제한다. --keep-files를 사용하면
성공한 CSV도 보존할 수 있다.

홈서버의 `compose.yml` MySQL 서비스에는 `--local-infile=1`이 설정되어
있어야 한다. 이미 실행 중인 컨테이너에는 설정이 자동으로 적용되지
않으므로 컨테이너를 재생성해야 한다. 원격 성능 테스트 DB를 사용하는
경우에도 서버 변수 `local_infile=ON`과 사용자 권한을 운영 정책에 맞게
확인한다.

## 검증

자동 검증:

    python3 /opt/pilgrimage/mock_data/generate_place_mock.py --config /opt/pilgrimage/mock_data/place_mock_config.json verify --profile small --database moonbackdb --host 127.0.0.1 --port 3306 --user YOUR_USER --password-env SPRING_DATASOURCE_PASSWORD --env-file /opt/pilgrimage/.env

다음 항목을 출력한다.

- namespace 총 건수
- content_type_id별 건수
- 주소 기반 지역별 건수
- description NULL/길이 bucket
- 좌표 최소/최대 및 전체 bounding box 밖 건수
- content_id 중복 건수
- 날짜 순서 오류 건수
- 공원/문화/별빛누리 이름 토큰 건수

수동 SQL 템플릿은 mock_data/validate_place_mock.sql이다. seed 또는
content_id namespace 설정을 변경하면 파일 상단의 범위 변수도 변경한다.

## 파일과 디스크 주의사항

- 설정: mock_data/place_mock_config.json
- 실행 코드: mock_data/generate_place_mock.py
- 검증 SQL: mock_data/validate_place_mock.sql
- 임시 CSV와 manifest: mock_data/generated
- 6백만 건의 CSV는 description과 한글 UTF-8 인코딩 때문에 수 GB가 될 수
  있다. 500,000건 chunk 하나도 수백 MB가 될 수 있으므로 출력 디스크의
  여유 공간을 확인한다.
- InnoDB 데이터와 content_id UNIQUE 인덱스는 CSV보다 더 큰 디스크를
  사용할 수 있다.
- 기본 run은 성공 chunk의 CSV를 삭제하지만 manifest는 보존한다.

## 데이터 삭제/초기화

도구는 데이터베이스의 기존 행을 삭제하는 명령을 제공하지 않는다. 명시적인
운영 승인 후 아래처럼 namespace를 먼저 확인하고 수동으로 삭제한다.

    SELECT COUNT(*) FROM place
    WHERE content_id BETWEEN 1838000000 AND 1843999999;
    DELETE FROM place
    WHERE content_id BETWEEN 1838000000 AND 1843999999;

place를 참조하는 다른 테스트 데이터가 있으면 FK 관계를 먼저 확인하고,
실서버에서는 백업과 승인 절차 없이 DELETE/TRUNCATE를 실행하지 않는다.

### init.sql로 테스트 DB 전체 초기화

이번처럼 `content_id` 타입을 바꾼 경우에는 기존 Place Mock 행만
지우는 것보다 승인된 테스트 DB를 `init.sql`로 다시 구성해야 한다. 이 파일은
여러 테이블을 DROP/CREATE하므로 운영 데이터가 있는 DB에서 실행하면 안 된다.

홈서버에서 백엔드와 프론트 컨테이너를 잠시 멈춘 뒤 실행한다.

    cd /opt/pilgrimage
    docker compose -f compose.yml stop backend frontend
    docker compose -f compose.yml cp mock_data/init.sql mysql:/tmp/place-init.sql
    docker compose -f compose.yml exec mysql mysql -uroot -p -e \
      "DROP DATABASE IF EXISTS moonbackdb; CREATE DATABASE moonbackdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    docker compose -f compose.yml exec mysql mysql -uroot -p -e \
      "source /tmp/place-init.sql"
    docker compose -f compose.yml start backend frontend

`docker compose down -v`는 MySQL 볼륨 자체를 삭제하므로 사용하지 않는다.
초기화 후에는 Generator의 `run --dry-run`으로 새 `int content_id` 스키마와
FK/제약조건을 확인한 다음 Mock Data를 적재한다.

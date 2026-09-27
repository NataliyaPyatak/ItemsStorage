#!/bin/bash
# Локальный тест навыка. Приложение должно быть запущено
# (run-конфигурация "ItemsStorageApplication (local)").
# Запуск: ./test.sh  или  bash test.sh
#
# Скрипт повторяет реальный диалог с Алисой и ПРОВЕРЯЕТ ответы:
# в каждом вызове post третий аргумент — кусок текста, который
# обязательно должен быть в ответе сервера. Несовпадение = FAIL.

URL=http://localhost:8080/

post() {
  local title="$1" expected="$2" body="$3"
  echo "=== $title ==="
  local resp
  resp=$(curl -s -X POST "$URL" -H "Content-Type: application/json" -d "$body")
  echo "$resp"
  if echo "$resp" | grep -q "$expected"; then
    echo "OK"
  else
    echo "FAIL — ожидалось: «$expected»"
  fi
  echo
}

# --- Шаг 1: старт сессии (new: true), навык должен спросить хранилище ---
post "1. Старт сессии" "Привет! Назови хранилище" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 0, "skill_id": "", "user": {"user_id": "local-test"}, "new": true},
  "request": {"type": "SimpleUtterance", "command": ""}
}'

# --- Шаг 2: называем хранилище (new: false, state пока пустой) ---
post "2. Назвали хранилище «гараж»" "Окей, хранилище: гараж" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 1, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "гараж"},
  "state": {"session": {"storage": ""}}
}'

# --- Шаг 3: дальше Алиса возвращает нам session_state со storage = "гараж" ---
post "3. Положить шуруповёрт в ящик 3" "Добавила: шуруповёрт в ящик 3" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 2, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "положил шуруповёрт в ящик 3"},
  "state": {"session": {"storage": "гараж"}}
}'

post "4. Где шуруповёрт" "шуруповёрт — ящик 3" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 3, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "где шуруповёрт"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 5: предложный падеж — Алиса возвращает «ящике», а сохранено «ящик» ---
post "5. Что в ящике 3 (предложный падеж)" "шуруповёрт" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 4, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "что в ящике 3"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 6: женский род глагола, новая вещь ---
post "6. Положила дрель в ящик 1" "Добавила: дрель в ящик 1" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 5, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "положила дрель в ящик 1"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 7: «что в» с предложным падежом находит по основе места ---
post "7. Что в ящике 1" "В ящике 1: дрель" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 6, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "что в ящике 1"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 8: «запиши, что … теперь в …» — обновляет место существующей вещи ---
post "8. Запиши, что дрель теперь в ящике 1" "Записала: дрель теперь в ящике 1" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 7, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "запиши, что дрель теперь в ящике 1"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 9: поиск вещи после «запиши» — в БД именительный падеж ---
post "9. Где дрель" "дрель — ящик 1" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 8, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "где дрель"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 10: винительный падеж вещи и места — в ответе как сказала ---
post "10. Положила кружку в сумку" "Добавила: кружка в сумку" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 9, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "положила кружку в сумку"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 11: в БД именительный падеж — «кружка — сумка», не «кружку в сумку» ---
post "11. Где кружка" "кружка — сумка" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 10, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "где кружка"},
  "state": {"session": {"storage": "гараж"}}
}'

post "12. Удалить шуруповёрт (чистит тестовые данные)" "Удалила шуруповёрт" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 11, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "удали шуруповёрт"},
  "state": {"session": {"storage": "гараж"}}
}'

post "13. Удалить дрель (чистит тестовые данные)" "Удалила дрель" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 12, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "удали дрель"},
  "state": {"session": {"storage": "гараж"}}
}'

post "14. Удалить кружку (чистит тестовые данные)" "Удалила кружка" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 13, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "удали кружку"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 15: чужой skill_id — пустой text и end_session: true ---
post "15. Чужой skill_id" '"text":""' '{
  "version": "1.0",
  "session": {"session_id": "t2", "message_id": 0, "skill_id": "чужой-навык", "user": {"user_id": "local-test"}, "new": true},
  "request": {"type": "SimpleUtterance", "command": "где шуруповёрт"},
  "state": {"session": {"storage": "гараж"}}
}'

# --- Шаг 16: выход ---
post "16. Выход" "Пока! Возвращайся" '{
  "version": "1.0",
  "session": {"session_id": "t1", "message_id": 14, "skill_id": "", "user": {"user_id": "local-test"}, "new": false},
  "request": {"type": "SimpleUtterance", "command": "выход"},
  "state": {"session": {"storage": "гараж"}}
}'

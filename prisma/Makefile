APP_ID ?= com.georgernstgraf.aitranscribe.debug
DB_NAME ?= aitranscribe.db
LOCAL_DB ?= device_db.sqlite

device/schema.prisma: device_db.sqlite
	cp template/schema.prisma device/
	(cd device && deno -A prisma db pull)

device_db.sqlite::
	@adb shell am force-stop "$(APP_ID)"
	@rm -f "$(LOCAL_DB)" "$(LOCAL_DB)-wal" "$(LOCAL_DB)-shm"
	@mkdir -p "$(dir $(LOCAL_DB))"
	@adb exec-out run-as "$(APP_ID)" cat "databases/$(DB_NAME)" > "$(LOCAL_DB)"
	@adb exec-out run-as "$(APP_ID)" cat "databases/$(DB_NAME)-wal" > "$(LOCAL_DB)-wal" || true
	@adb exec-out run-as "$(APP_ID)" cat "databases/$(DB_NAME)-shm" > "$(LOCAL_DB)-shm" || true
	@echo "Stopped app, cleaned local copies, and pulled DB (+ WAL/SHM if present)"

-- 좌석당 활성 예약 1건만 허용
-- 취소된 예약은 행이 남으므로 유니크 대상에서 제외해 좌석 재판매 허용
create unique index uk_reservation_active_seat
    on reservations (seat_id)
    where status <> 'CANCELLED';

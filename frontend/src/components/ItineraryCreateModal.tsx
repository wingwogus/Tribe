import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { addDays, format } from "date-fns";

interface ItineraryCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateItinerary: (data: {
    placeId?: number | null;
    title?: string | null;
    time?: string | null;
    memo?: string | null;
  }) => void;
  categoryName: string;
  tripStartDate: string; // ISO date string (YYYY-MM-DD)
  currentDay: number; // Day number (1, 2, 3, ...)
}

export const ItineraryCreateModal = ({
  isOpen,
  onClose,
  onCreateItinerary,
  categoryName,
  tripStartDate,
  currentDay,
}: ItineraryCreateModalProps) => {
  const [title, setTitle] = useState("");
  const [timeInput, setTimeInput] = useState(""); // HH:mm format
  const [memo, setMemo] = useState("");

  const handleSubmit = () => {
    if (!title.trim()) {
      return;
    }

    // Calculate the date for this day
    const startDate = new Date(tripStartDate);
    const targetDate = addDays(startDate, currentDay - 1);
    
    // Combine date with time if time is provided
    let isoDateTime = null;
    if (timeInput) {
      const [hours, minutes] = timeInput.split(':');
      targetDate.setHours(parseInt(hours), parseInt(minutes), 0, 0);
      isoDateTime = targetDate.toISOString();
    }

    onCreateItinerary({
      placeId: null,
      title: title,
      time: isoDateTime,
      memo: memo || null,
    });

    // Reset form
    setTitle("");
    setTimeInput("");
    setMemo("");
    onClose();
  };

  const handleClose = () => {
    setTitle("");
    setTimeInput("");
    setMemo("");
    onClose();
  };

  // Calculate display date
  const startDate = new Date(tripStartDate);
  const targetDate = addDays(startDate, currentDay - 1);
  const displayDate = format(targetDate, 'yyyy년 M월 d일');

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>일정 추가</DialogTitle>
          <DialogDescription>
            {categoryName}에 새로운 일정을 추가합니다
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="title">일정 제목 *</Label>
            <Input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="예: 호텔에서 휴식, 점심 식사"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="time">시간 (선택)</Label>
            <div className="text-sm text-muted-foreground mb-2">
              날짜: {displayDate}
            </div>
            <Input
              id="time"
              type="time"
              value={timeInput}
              onChange={(e) => setTimeInput(e.target.value)}
              placeholder="HH:MM"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="memo">메모 (선택)</Label>
            <Textarea
              id="memo"
              value={memo}
              onChange={(e) => setMemo(e.target.value)}
              placeholder="메모를 입력하세요"
              rows={3}
            />
          </div>
        </div>

        <div className="flex justify-end space-x-2">
          <Button variant="outline" onClick={handleClose}>
            취소
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!title.trim()}
          >
            추가
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

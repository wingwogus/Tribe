import {useNavigate} from "react-router-dom";
import {useMutation, useQueryClient} from "@tanstack/react-query";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import {z} from "zod";
import {format} from "date-fns";
import {CalendarIcon, Loader2} from "lucide-react";
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Calendar} from "@/components/ui/calendar";
import {Popover, PopoverContent, PopoverTrigger} from "@/components/ui/popover";
import {useToast} from "@/hooks/use-toast";
import {ImportTripRequest, tripApi} from "@/api/trips";
import {cn} from "@/lib/utils";

interface TripImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  postId: number;
  originalTitle: string;
  originalStartDate: string;
  originalEndDate: string;
}

const importTripSchema = z.object({
  title: z.string().min(1, "여행 제목을 입력해주세요").max(100, "제목은 100자 이내로 입력해주세요"),
  startDate: z.date({ required_error: "시작일을 선택해주세요" }),
  endDate: z.date({ required_error: "종료일을 선택해주세요" }),
}).refine((data) => data.startDate <= data.endDate, {
  message: "시작일은 종료일보다 이전이거나 같아야 합니다",
  path: ["endDate"],
});

type ImportTripFormData = z.infer<typeof importTripSchema>;

export const TripImportModal = ({
  isOpen,
  onClose,
  postId,
  originalTitle,
  originalStartDate,
  originalEndDate,
}: TripImportModalProps) => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const form = useForm<ImportTripFormData>({
    resolver: zodResolver(importTripSchema),
    defaultValues: {
      title: `${originalTitle} (복사)`,
      startDate: new Date(originalStartDate),
      endDate: new Date(originalEndDate),
    },
  });

  const mutation = useMutation({
    mutationFn: (data: ImportTripRequest) => tripApi.importTrip(data),
    onSuccess: (data) => {
      toast({
        title: "여행 복사 완료!",
        description: `${data.title} 여행이 생성되었습니다.`,
      });
      onClose();
      navigate(`/trip/${data.tripId}`);
      queryClient.invalidateQueries({ queryKey: ['trips'] });
    },
    onError: (error: any) => {
      toast({
        title: "여행 복사 실패",
        description: error.response?.data?.message || "오류가 발생했습니다",
        variant: "destructive",
      });
    },
  });

  const onSubmit = (data: ImportTripFormData) => {
    const request: ImportTripRequest = {
      postId,
      title: data.title,
      startDate: format(data.startDate, 'yyyy-MM-dd'),
      endDate: format(data.endDate, 'yyyy-MM-dd'),
    };
    mutation.mutate(request);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>여행 복사하기</DialogTitle>
          <DialogDescription>
            이 여행 일정을 복사하여 나만의 여행을 계획할 수 있습니다.
            일정과 장소 정보가 복사되며, 날짜는 자유롭게 변경 가능합니다.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 mt-4">
          {/* Title Input */}
          <div className="space-y-2">
            <Label htmlFor="title">여행 제목</Label>
            <Input
              id="title"
              {...form.register("title")}
              placeholder="여행 제목을 입력하세요"
              className={form.formState.errors.title ? "border-destructive" : ""}
            />
            {form.formState.errors.title && (
              <p className="text-sm text-destructive">{form.formState.errors.title.message}</p>
            )}
          </div>

          {/* Start Date */}
          <div className="space-y-2">
            <Label>여행 시작일</Label>
            <Popover>
              <PopoverTrigger asChild>
                <Button
                  variant="outline"
                  className={cn(
                    "w-full justify-start text-left font-normal",
                    !form.watch("startDate") && "text-muted-foreground"
                  )}
                >
                  <CalendarIcon className="mr-2 h-4 w-4" />
                  {form.watch("startDate") ? (
                    format(form.watch("startDate"), "PPP")
                  ) : (
                    <span>시작일 선택</span>
                  )}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <Calendar
                  mode="single"
                  selected={form.watch("startDate")}
                  onSelect={(date) => form.setValue("startDate", date as Date)}
                  initialFocus
                  className="pointer-events-auto"
                />
              </PopoverContent>
            </Popover>
            {form.formState.errors.startDate && (
              <p className="text-sm text-destructive">{form.formState.errors.startDate.message}</p>
            )}
          </div>

          {/* End Date */}
          <div className="space-y-2">
            <Label>여행 종료일</Label>
            <Popover>
              <PopoverTrigger asChild>
                <Button
                  variant="outline"
                  className={cn(
                    "w-full justify-start text-left font-normal",
                    !form.watch("endDate") && "text-muted-foreground"
                  )}
                >
                  <CalendarIcon className="mr-2 h-4 w-4" />
                  {form.watch("endDate") ? (
                    format(form.watch("endDate"), "PPP")
                  ) : (
                    <span>종료일 선택</span>
                  )}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <Calendar
                  mode="single"
                  selected={form.watch("endDate")}
                  onSelect={(date) => form.setValue("endDate", date as Date)}
                  initialFocus
                  className="pointer-events-auto"
                />
              </PopoverContent>
            </Popover>
            {form.formState.errors.endDate && (
              <p className="text-sm text-destructive">{form.formState.errors.endDate.message}</p>
            )}
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-2 mt-6">
            <Button type="button" variant="outline" onClick={onClose} disabled={mutation.isPending}>
              취소
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  복사 중...
                </>
              ) : (
                "복사하기"
              )}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};

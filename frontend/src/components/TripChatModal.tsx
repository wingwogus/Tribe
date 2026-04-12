import {useEffect, useMemo, useRef, useState} from 'react';
import {type InfiniteData, useInfiniteQuery, useMutation, useQueryClient,} from '@tanstack/react-query';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Avatar, AvatarFallback, AvatarImage} from '@/components/ui/avatar';
import {useToast} from '@/hooks/use-toast';
import {chatApi, type ChatHistoryResponse, type ChatMessage} from '@/api/chat';
import {format} from 'date-fns';
import {Send, X} from 'lucide-react';
import {cn} from '@/lib/utils';
import { tripQueryKeys } from '@/lib/tripQueryKeys';

interface TripChatModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tripId: number;
  tripTitle?: string;
  currentMemberId: number | null;
  myTripMemberId: number | null;
}

export const TripChatModal = ({
  open,
  onOpenChange,
  tripId,
  tripTitle,
  currentMemberId,
  myTripMemberId,
}: TripChatModalProps) => {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [inputValue, setInputValue] = useState('');
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);
  const prevScrollHeightRef = useRef<number | null>(null);
  const prevScrollTopRef = useRef<number | null>(null);
  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery<ChatHistoryResponse>({
    queryKey: tripQueryKeys.chat(tripId),
    queryFn: ({ pageParam }) =>
      chatApi.getChatHistory(tripId, pageParam as string | undefined),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.nextCursor ?? undefined : undefined,
    enabled: open && !!tripId,
    staleTime: 5000,
  });

  const messages: ChatMessage[] = useMemo(() => {
    if (!data?.pages) return [];
    const flat = data.pages.flatMap((page) => page.content);
    return [...flat].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
    );
  }, [data]);

  const sendChatMutation = useMutation({
    mutationFn: (content: string) => chatApi.sendChat(tripId, content),
    onSuccess: (newMessage) => {
      setInputValue('');

      queryClient.setQueryData<InfiniteData<ChatHistoryResponse> | undefined>(
        tripQueryKeys.chat(tripId),
        (oldData: InfiniteData<ChatHistoryResponse> | undefined) => {
          if (!oldData) {
            return {
              pageParams: [undefined],
              pages: [
                {
                  content: [newMessage],
                  nextCursor: null,
                  hasNext: false,
                },
              ],
            };
          }

          const pages = [...oldData.pages];
          const lastPage = pages[pages.length - 1];

          const updatedLastPage: ChatHistoryResponse = {
            ...lastPage,
            content: [...lastPage.content, newMessage],
          };

          pages[pages.length - 1] = updatedLastPage;

          return {
            ...oldData,
            pages,
          };
        },
      );

      // 새 메시지로 스크롤 하단 이동
      setTimeout(() => {
        if (messagesContainerRef.current) {
          messagesContainerRef.current.scrollTop =
            messagesContainerRef.current.scrollHeight;
        }
      }, 0);
    },
    onError: () => {
      toast({
        title: '전송 실패',
        description: '메시지 전송 중 오류가 발생했습니다.',
        variant: 'destructive',
      });
    },
  });

  const handleSend = () => {
    const trimmed = inputValue.trim();
    if (!trimmed || sendChatMutation.isPending) return;
    sendChatMutation.mutate(trimmed);
  };

  const handleSubmit = (e: any) => {
    e.preventDefault();
    handleSend();
  };

  const handleScroll = async () => {
    const container = messagesContainerRef.current;
    if (!container) return;

    if (!hasNextPage || isFetchingNextPage) return;

    if (container.scrollTop <= 40) {
      prevScrollHeightRef.current = container.scrollHeight;
      prevScrollTopRef.current = container.scrollTop;

      try {
        await fetchNextPage();
      } finally {
        setTimeout(() => {
          const c = messagesContainerRef.current;
          if (
            !c ||
            prevScrollHeightRef.current == null ||
            prevScrollTopRef.current == null
          ) {
            return;
          }

          const newScrollHeight = c.scrollHeight;
          const diff = newScrollHeight - prevScrollHeightRef.current;

          c.scrollTop = prevScrollTopRef.current + diff;

          prevScrollHeightRef.current = null;
          prevScrollTopRef.current = null;
        }, 0);
      }
    }
  };
  const isMyMessage = (msg: ChatMessage) => {
    if (!currentMemberId && !myTripMemberId) return false;
    return (
      (currentMemberId != null && msg.sender.memberId === currentMemberId) ||
      (myTripMemberId != null && msg.sender.tripMemberId === myTripMemberId)
    );
  };

  const isSameSender = (
    a: ChatMessage | undefined,
    b: ChatMessage | undefined,
  ) => {
    if (!a || !b) return false;

    if (a.sender.memberId != null && b.sender.memberId != null) {
      return a.sender.memberId === b.sender.memberId;
    }

    if (a.sender.tripMemberId != null && b.sender.tripMemberId != null) {
      return a.sender.tripMemberId === b.sender.tripMemberId;
    }

    return a.sender.nickname === b.sender.nickname;
  };

  const isSameMinute = (
    a: ChatMessage | undefined,
    b: ChatMessage | undefined,
  ) => {
    if (!a || !b) return false;

    const t1 = new Date(a.timestamp).getTime();
    const t2 = new Date(b.timestamp).getTime();

    if (Number.isNaN(t1) || Number.isNaN(t2)) return false;

    return Math.floor(t1 / 600000) === Math.floor(t2 / 600000);
  };

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return timestamp;
    return format(date, 'MM.dd HH:mm');
  };

  const urlRegex = /(https?:\/\/[^\s]+)/g;

  const renderMessageContent = (content: string) => {
    const parts = content.split(urlRegex);

    return parts.map((part, index) => {
      if (!part) return null;

      if (/^https?:\/\/\S+$/.test(part)) {
        return (
          <a
            key={`link-${index}`}
            href={part}
            target="_blank"
            rel="noopener noreferrer"
            className="text-black break-all hover:underline"
          >
            {part}
          </a>
        );
      }

      const lines = part.split('\n');

      return lines.map((line, lineIndex) => (
        <span key={`text-${index}-${lineIndex}`}>
          {line}
          {lineIndex < lines.length - 1 && <br />}
        </span>
      ));
    });
  };


  const initialScrollDoneRef = useRef(false);

  useEffect(() => {
    if (!open) {
      setInputValue('');
      initialScrollDoneRef.current = false;
      return;
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    if (initialScrollDoneRef.current) return;
    if (messages.length === 0) return;

    // 처음 메시지가 로딩되었을 때만 최신 메시지 하단으로 스크롤
    setTimeout(() => {
      if (messagesContainerRef.current) {
        messagesContainerRef.current.scrollTop =
          messagesContainerRef.current.scrollHeight;
        initialScrollDoneRef.current = true;
      }
    }, 0);
  }, [open, messages.length]);

  if (!open) {
    return null;
  }

  return (
    <div className="fixed bottom-4 right-4 z-40 w-[calc(100%-2rem)] max-w-md sm:bottom-6 sm:right-6">
      <div className="flex flex-col h-[60vh] max-h-[520px] min-h-[320px] bg-background border rounded-2xl shadow-xl p-4 sm:p-5 ">
        <div className="pb-2 flex items-center justify-between border-b-2">
          <div>
            <h2 className="text-base sm:text-lg font-semibold">
              {tripTitle ? `${tripTitle}` : ''}
            </h2>
            <p className="text-xs sm:text-sm text-muted-foreground">
              이 여행 멤버들과 실시간으로 대화해보세요.
            </p>
          </div>
          <button
            type="button"
            onClick={() => onOpenChange(false)}
            className="ml-2 rounded-full p-1 text-muted-foreground hover:bg-muted"
            aria-label="채팅 닫기"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div
          ref={messagesContainerRef}
          onScroll={handleScroll}
          className="flex-1 overflow-y-auto pr-1 py-2"
        >
          {isLoading ? (
            <p className="text-xs text-muted-foreground text-center mt-4">
              채팅을 불러오는 중입니다...
            </p>
          ) : isError ? (
            <p className="text-xs text-destructive text-center mt-4">
              채팅을 불러오는 중 오류가 발생했습니다.
            </p>
          ) : messages.length === 0 ? (
            <p className="text-xs text-muted-foreground text-center mt-4">
              아직 대화가 없습니다. 첫 메시지를 보내보세요!
            </p>
          ) : (
            <>
              {isFetchingNextPage && (
                <p className="text-[10px] text-muted-foreground text-center mb-1">
                  이전 메시지를 불러오는 중...
                </p>
              )}

              {messages.map((msg, index) => {
                const mine = isMyMessage(msg);
                const prev = index > 0 ? messages[index - 1] : undefined;
                const next =
                  index < messages.length - 1
                    ? messages[index + 1]
                    : undefined;

                const groupedWithPrev =
                  isSameSender(prev, msg) && isSameMinute(prev, msg);
                const groupedWithNext =
                  isSameSender(next, msg) && isSameMinute(next, msg);

                const showAvatar = !groupedWithPrev;
                const showNickname = !mine && !groupedWithPrev;
                const showTime = !groupedWithNext;

                const isFirstInGroup = !groupedWithPrev;
                const isLastInGroup = !groupedWithNext;
                const isSingleInGroup = isFirstInGroup && isLastInGroup;

                let bubbleRadiusClasses = 'rounded-2xl';

                if (!isSingleInGroup) {
                  if (!mine) {
                    if (isFirstInGroup && !isLastInGroup) {
                      bubbleRadiusClasses = 'rounded-2xl rounded-bl-sm';
                    } else if (!isFirstInGroup && !isLastInGroup) {
                      bubbleRadiusClasses =
                        'rounded-r-2xl rounded-tl-sm rounded-bl-sm';
                    } else if (!isFirstInGroup && isLastInGroup) {
                      bubbleRadiusClasses = 'rounded-2xl rounded-tl-sm';
                    }
                  } else {
                    if (isFirstInGroup && !isLastInGroup) {
                      bubbleRadiusClasses = 'rounded-2xl rounded-br-sm';
                    } else if (!isFirstInGroup && !isLastInGroup) {
                      bubbleRadiusClasses =
                        'rounded-l-2xl rounded-tr-sm rounded-br-sm';
                    } else if (!isFirstInGroup && isLastInGroup) {
                      bubbleRadiusClasses = 'rounded-2xl rounded-tr-sm';
                    }
                  }
                }

                return (
                  <div
                    key={msg.messageId}
                    className={cn(
                      'flex items-end gap-1.5',
                      mine ? 'justify-end' : 'justify-start',
                      groupedWithPrev ? 'mt-0.5' : 'mt-4',
                    )}
                  >
                    {!mine && (
                      showAvatar ? (
                        <Avatar className="h-7 w-7">
                          <AvatarImage
                            src={msg.sender.avatar || undefined}
                            alt={msg.sender.nickname}
                            className="object-cover"
                          />
                          <AvatarFallback className="text-[10px] bg-primary/10 text-primary">
                            {msg.sender.nickname?.[0]?.toUpperCase() || '?'}
                          </AvatarFallback>
                        </Avatar>
                      ) : (
                        <div className="h-7 w-7" />
                      )
                    )}

                    <div
                      className={cn(
                        'flex max-w-[70%] flex-col gap-1',
                        mine ? 'items-end' : 'items-start',
                      )}
                    >
                      <div
                        className={cn(
                          'px-3 py-2 text-xs sm:text-sm break-all',
                          bubbleRadiusClasses,
                          mine
                            ? 'bg-primary text-primary-foreground'
                            : 'bg-muted text-foreground',
                        )}
                      >
                        {showNickname && (
                          <p className="text-[10px] text-muted-foreground mb-0.5">
                            {msg.sender.nickname}
                          </p>
                        )}
                        <p>{renderMessageContent(msg.content)}</p>
                      </div>
                      {showTime && (
                        <p className="text-[10px] text-muted-foreground">
                          {formatTime(msg.timestamp)}
                        </p>
                      )}
                    </div>

                    {mine && (
                      showAvatar ? (
                        <Avatar className="h-7 w-7">
                          <AvatarImage
                            src={msg.sender.avatar || undefined}
                            alt={msg.sender.nickname}
                            className="object-cover"
                          />
                          <AvatarFallback className="text-[10px] bg-primary/10 text-primary">
                            {msg.sender.nickname?.[0]?.toUpperCase() || '?'}
                          </AvatarFallback>
                        </Avatar>
                      ) : (
                        <div className="h-7 w-7" />
                      )
                    )}
                  </div>
                );
              })}
            </>
          )}
        </div>

        <form className="mt-3 flex items-center gap-2" onSubmit={handleSubmit}>
          <Input
            placeholder="메시지를 입력하세요..."
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            className="text-sm"
          />
          <Button
            size="icon"
            className="rounded-full"
            type="submit"
            disabled={!inputValue.trim() || sendChatMutation.isPending}
          >
            <Send className="w-4 h-4" />
          </Button>
        </form>
      </div>
    </div>
  );
};

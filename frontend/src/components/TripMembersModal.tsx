import {useState} from "react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {useNavigate} from "react-router-dom";
import {Check, Copy, LogOut, MoreVertical, Shield, Trash2, UserMinus, Users} from "lucide-react";
import {tripApi, TripRole} from "@/api/trips";
import {getMemberInfo} from "@/api/auth";
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Avatar, AvatarFallback, AvatarImage} from "@/components/ui/avatar";
import {Badge} from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {Input} from "@/components/ui/input";
import {useToast} from "@/hooks/use-toast";
import { tripQueryKeys } from "@/lib/tripQueryKeys";

interface TripMembersModalProps {
  isOpen: boolean;
  onClose: () => void;
  tripId: number;
}

export function TripMembersModal({
  isOpen,
  onClose,
  tripId,
}: TripMembersModalProps) {
  const { toast } = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  const [inviteLink, setInviteLink] = useState("");
  const [isCopied, setIsCopied] = useState(false);
  const [showLeaveDialog, setShowLeaveDialog] = useState(false);
  const [showKickDialog, setShowKickDialog] = useState<{ memberId: number; name: string } | null>(null);
  const [showDeleteGuestDialog, setShowDeleteGuestDialog] = useState<{ guestId: number; name: string } | null>(null);

  // Fetch trip detail directly
  const { data: tripDetail, isLoading } = useQuery({
    queryKey: tripQueryKeys.trip(tripId),
    queryFn: () => tripApi.getTripById(tripId),
    enabled: isOpen,
  });

  // Get current user info
  const { data: currentUser } = useQuery({
    queryKey: ['currentUser'],
    queryFn: getMemberInfo,
  });

  const currentUserId = currentUser?.memberId || null;

  // Find current user's role
  const currentMember = tripDetail?.members.find(m => m.memberId === currentUserId);
  const isOwner = currentMember?.role === 'OWNER';
  const isAdmin = currentMember?.role === 'ADMIN';
  const canManageMembers = isOwner || isAdmin;

  // Generate invite link mutation
  const generateInviteMutation = useMutation({
    mutationFn: () => tripApi.generateInvite(tripId),
    onSuccess: (data) => {
      setInviteLink(data.inviteLink);
      toast({
        title: "초대 링크 생성 완료",
        description: "링크를 복사하여 친구에게 공유하세요",
      });
    },
    onError: () => {
      toast({
        title: "초대 링크 생성 실패",
        description: "다시 시도해주세요",
        variant: "destructive",
      });
    },
  });

  // Assign role mutation
  const assignRoleMutation = useMutation({
    mutationFn: ({ memberId, newRole }: { memberId: number; newRole: 'ADMIN' | 'MEMBER' }) =>
      tripApi.assignRole(tripId, memberId, { requestRole: newRole }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.trip(tripId) });
      toast({
        title: "권한 변경 완료",
        description: "멤버 권한이 변경되었습니다",
      });
    },
    onError: () => {
      toast({
        title: "권한 변경 실패",
        description: "다시 시도해주세요",
        variant: "destructive",
      });
    },
  });

  // Kick member mutation
  const kickMemberMutation = useMutation({
    mutationFn: (memberId: number) => tripApi.kickMember(tripId, memberId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.trip(tripId) });
      setShowKickDialog(null);
      toast({
        title: "멤버 강퇴 완료",
        description: "멤버가 여행에서 제외되었습니다",
      });
    },
    onError: () => {
      toast({
        title: "멤버 강퇴 실패",
        description: "다시 시도해주세요",
        variant: "destructive",
      });
    },
  });

  // Delete guest mutation
  const deleteGuestMutation = useMutation({
    mutationFn: (guestId: number) => tripApi.deleteGuest(tripId, guestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.trip(tripId) });
      setShowDeleteGuestDialog(null);
      toast({
        title: "게스트 삭제 완료",
        description: "게스트가 삭제되었습니다",
      });
    },
    onError: () => {
      toast({
        title: "게스트 삭제 실패",
        description: "다시 시도해주세요",
        variant: "destructive",
      });
    },
  });

  // Leave trip mutation
  const leaveTripMutation = useMutation({
    mutationFn: () => tripApi.leaveTrip(tripId),
    onSuccess: () => {
      toast({
        title: "여행 탈퇴 완료",
        description: "여행에서 탈퇴했습니다",
      });
      navigate('/');
    },
    onError: () => {
      toast({
        title: "여행 탈퇴 실패",
        description: "다시 시도해주세요",
        variant: "destructive",
      });
    },
  });

  const handleCopyInviteLink = async () => {
    try {
      await navigator.clipboard.writeText(inviteLink);
      setIsCopied(true);
      toast({
        title: "복사 완료",
        description: "초대 링크가 클립보드에 복사되었습니다",
      });
      setTimeout(() => setIsCopied(false), 2000);
    } catch (error) {
      toast({
        title: "복사 실패",
        description: "다시 시도해주세요",
        variant: "destructive",
      });
    }
  };

  const handleGenerateInvite = () => {
    generateInviteMutation.mutate();
  };

  const handleRoleChange = (memberId: number, currentRole: TripRole) => {
    const newRole: 'ADMIN' | 'MEMBER' = currentRole === 'ADMIN' ? 'MEMBER' : 'ADMIN';
    assignRoleMutation.mutate({ memberId, newRole });
  };

  // Helper function for role badges
  const getRoleBadge = (role: TripRole) => {
    const roleConfig: Record<TripRole, { label: string; variant: 'default' | 'secondary' | 'outline' | 'destructive'; className: string }> = {
      OWNER: { label: 'OWNER', variant: 'default', className: 'bg-purple-600 hover:bg-purple-700 border-0' },
      ADMIN: { label: 'ADMIN', variant: 'default', className: 'bg-blue-600 hover:bg-blue-700 border-0' },
      MEMBER: { label: 'MEMBER', variant: 'secondary', className: '' },
      GUEST: { label: 'GUEST', variant: 'outline', className: 'border-orange-400 text-orange-700' },
      EXITED: { label: '탈퇴', variant: 'outline', className: 'border-gray-400 text-gray-500' },
      KICKED: { label: '강퇴됨', variant: 'destructive', className: '' }
    };
    
    const config = roleConfig[role];
    return (
      <Badge variant={config.variant} className={`text-xs mt-1 ${config.className}`}>
        {config.label}
      </Badge>
    );
  };

  // Filter and sort members
  const activeMembers = tripDetail?.members.filter(
    m => m.role !== 'EXITED' && m.role !== 'KICKED'
  ) || [];

  const roleOrder: Record<string, number> = { OWNER: 0, ADMIN: 1, MEMBER: 2, GUEST: 3 };
  const sortedActiveMembers = [...activeMembers].sort((a, b) => {
    return (roleOrder[a.role] ?? 99) - (roleOrder[b.role] ?? 99);
  });

  // Show loading state
  if (!tripDetail || isLoading) {
    return (
      <Dialog open={isOpen} onOpenChange={onClose}>
        <DialogContent className="max-w-lg">
          <div className="flex items-center justify-center p-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <>
      <Dialog open={isOpen} onOpenChange={onClose}>
        <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Users className="w-5 h-5" />
              멤버 관리
            </DialogTitle>
            <DialogDescription>
              여행 멤버를 관리하고 초대 링크를 생성하세요
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {/* Members List */}
            <div className="space-y-2">
              <h3 className="text-sm font-medium text-muted-foreground">멤버 목록 ({sortedActiveMembers.length})</h3>
              <div className="space-y-2">
                {sortedActiveMembers.map((member) => {
                  const isCurrentUser = member.memberId === currentUserId;
                  const isGuest = member.role === 'GUEST';
                  
                  return (
                    <div
                      key={`${member.tripMemberId}-${member.nickname}`}
                      className="flex items-center justify-between p-3 rounded-lg border bg-card hover:bg-accent/50 transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <Avatar className="w-10 h-10">
                          <AvatarImage
                              src={member.avatar || undefined}
                              alt={member.nickname}
                              className="object-cover"
                          />
                          <AvatarFallback className="bg-primary text-primary-foreground">
                            {member.nickname.charAt(0).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-medium">{member.nickname}</span>
                            {isCurrentUser && (
                              <Badge variant="secondary" className="text-xs">나</Badge>
                            )}
                          </div>
                          {getRoleBadge(member.role)}
                        </div>
                      </div>

                      {/* Actions - OWNER can manage all, ADMIN can manage MEMBER only */}
                      {canManageMembers && !isCurrentUser && (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon">
                              <MoreVertical className="w-4 h-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            {!isGuest && (
                              <>
                                {/* Only OWNER can change roles, and only for ADMIN/MEMBER (not other OWNERs) */}
                                {isOwner && member.role !== 'OWNER' && member.memberId != null && (
                                  <>
                                    <DropdownMenuItem
                                      onClick={() => handleRoleChange(member.memberId, member.role)}
                                    >
                                      <Shield className="w-4 h-4 mr-2" />
                                      {member.role === 'ADMIN' ? 'MEMBER로 변경' : 'ADMIN으로 변경'}
                                    </DropdownMenuItem>
                                    <DropdownMenuSeparator />
                                  </>
                                )}
                                {/* OWNER can kick anyone except other OWNERs, ADMIN can only kick MEMBERs */}
                                {(((isOwner && member.role !== 'OWNER') || (isAdmin && member.role === 'MEMBER')) && member.memberId != null) ? (
                                  <DropdownMenuItem
                                    onClick={() => setShowKickDialog({ memberId: member.memberId, name: member.nickname })}
                                    className="text-destructive"
                                  >
                                    <UserMinus className="w-4 h-4 mr-2" />
                                    멤버 강퇴
                                  </DropdownMenuItem>
                                ) : null}
                              </>
                            )}
                            {/* Both OWNER and ADMIN can delete guests */}
                            {isGuest && (
                              <DropdownMenuItem
                                onClick={() => setShowDeleteGuestDialog({ guestId: member.tripMemberId!, name: member.nickname })}
                                className="text-destructive"
                              >
                                <Trash2 className="w-4 h-4 mr-2" />
                                게스트 삭제
                              </DropdownMenuItem>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Invite Link Section */}
            {inviteLink && (
              <div className="space-y-2">
                <h3 className="text-sm font-medium text-muted-foreground">초대 링크</h3>
                <div className="flex gap-2">
                  <Input
                    value={inviteLink}
                    readOnly
                    className="flex-1"
                  />
                  <Button
                    onClick={handleCopyInviteLink}
                    variant="outline"
                    className="min-w-[100px]"
                  >
                    {isCopied ? (
                      <>
                        <Check className="w-4 h-4 mr-2" />
                        복사됨
                      </>
                    ) : (
                      <>
                        <Copy className="w-4 h-4 mr-2" />
                        복사
                      </>
                    )}
                  </Button>
                </div>
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex flex-col sm:flex-row gap-2 pt-4 border-t">
              <Button
                onClick={handleGenerateInvite}
                disabled={generateInviteMutation.isPending}
                className="flex-1"
                variant="outline"
              >
                <Users className="w-4 h-4 mr-2" />
                초대 링크 생성
              </Button>
              
              {!isOwner && (
                <Button
                  onClick={() => setShowLeaveDialog(true)}
                  variant="destructive"
                  className="flex-1"
                >
                  <LogOut className="w-4 h-4 mr-2" />
                  여행 탈퇴
                </Button>
              )}
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Leave Trip Confirmation Dialog */}
      <AlertDialog open={showLeaveDialog} onOpenChange={setShowLeaveDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>여행을 탈퇴하시겠습니까?</AlertDialogTitle>
            <AlertDialogDescription>
              여행에서 탈퇴하면 더 이상 이 여행의 일정과 정보를 볼 수 없습니다.
              이 작업은 취소할 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => leaveTripMutation.mutate()}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              탈퇴하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Kick Member Confirmation Dialog */}
      <AlertDialog open={!!showKickDialog} onOpenChange={() => setShowKickDialog(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>멤버를 강퇴하시겠습니까?</AlertDialogTitle>
            <AlertDialogDescription>
              {showKickDialog?.name}님을 여행에서 제외합니다.
              이 작업은 취소할 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => showKickDialog && kickMemberMutation.mutate(showKickDialog.memberId)}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              강퇴하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Delete Guest Confirmation Dialog */}
      <AlertDialog open={!!showDeleteGuestDialog} onOpenChange={() => setShowDeleteGuestDialog(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>게스트를 삭제하시겠습니까?</AlertDialogTitle>
            <AlertDialogDescription>
              {showDeleteGuestDialog?.name} 게스트를 삭제합니다.
              이 작업은 취소할 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => showDeleteGuestDialog && deleteGuestMutation.mutate(showDeleteGuestDialog.guestId)}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              삭제하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}

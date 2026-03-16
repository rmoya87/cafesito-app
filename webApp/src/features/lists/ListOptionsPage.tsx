import { Button, cn } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";
import type { UserRow, UserListRow, ListPrivacy } from "../../types";
import type { ListMemberRow, ListInvitationRow } from "../../data/supabaseApi";
import { LIST_PRIVACY_OPTIONS } from "./listPrivacyOptions";
import { ListOptionsMembersBlock } from "./ListOptionsMembersBlock";
import { Switch } from "../../ui/components";

/**
 * Página de opciones de lista (privacidad, miembros, general).
 * Sustituye la modal; el título y botón atrás van en el TopBar.
 */
export function ListOptionsPage({
  list,
  isOwner,
  listPrivacy,
  listMembersCanEdit,
  listMembersCanInvite = false,
  onPrivacyChange,
  listOptionsMembers,
  listOptionsMemberUsers,
  users,
  currentUserId,
  shareUrl,
  onInvite,
  invitingId,
  onCopyLink,
  onRemoveMember,
  showCopyChip,
  copyChipExiting,
  onEditList,
  onDeleteList,
  onLeaveList,
  mutualFollowIds,
  invitations = []
}: {
  list: UserListRow | null;
  isOwner: boolean;
  listPrivacy: ListPrivacy;
  listMembersCanEdit: boolean;
  /** Solo aplica cuando privacy es invitation. Por defecto false. */
  listMembersCanInvite?: boolean;
  onPrivacyChange: (privacy: ListPrivacy, membersCanEdit: boolean, membersCanInvite?: boolean) => Promise<void>;
  listOptionsMembers: ListMemberRow[];
  listOptionsMemberUsers: UserRow[];
  users: UserRow[];
  currentUserId: number;
  shareUrl: string;
  onInvite: (userId: number) => Promise<void>;
  invitingId: number | null;
  onCopyLink: () => void;
  onRemoveMember: (userId: number) => Promise<void>;
  showCopyChip: boolean;
  copyChipExiting: boolean;
  onEditList: () => void;
  onDeleteList: () => void;
  onLeaveList: () => void;
  /** IDs de usuarios con follow mutuo (para listas públicas: terceros solo ven en Miembros a estos). */
  mutualFollowIds?: Set<number>;
  /** Invitaciones pendientes (para mostrar "Invitación enviada" y filtrar sugerencias). */
  invitations?: ListInvitationRow[];
}) {
  return (
    <section className="list-options-page profile-users-list-view" aria-label="Opciones de lista">
      <div className="list-options-page-content">
        {isOwner && (
          <div className="list-options-page-section list-options-privacy">
            <h3 className="create-list-privacy-subtitle">Privacidad</h3>
            <div className="create-list-privacy-card">
              <div className="create-list-privacy-options">
                {LIST_PRIVACY_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    className={cn("create-list-privacy-option-btn", listPrivacy === opt.value && "is-selected")}
                    onClick={() => void onPrivacyChange(opt.value, listMembersCanEdit, listMembersCanInvite)}
                    aria-pressed={listPrivacy === opt.value}
                  >
                    <span className="create-list-privacy-option-check">
                      {listPrivacy === opt.value ? (
                        <UiIcon name="check-circle-filled" aria-hidden="true" />
                      ) : (
                        <span className="create-list-privacy-option-check-empty" aria-hidden="true" />
                      )}
                    </span>
                    <div className="create-list-privacy-option-text">
                      <span className="create-list-privacy-option-label">{opt.label}</span>
                      <span className="create-list-privacy-option-desc">{opt.description}</span>
                    </div>
                  </button>
                ))}
              </div>
              {listPrivacy === "invitation" && (
                <div
                  className="share-list-privacy-option-switch-row create-list-privacy-option-switch-row"
                  onClick={(e) => e.stopPropagation()}
                >
                  <span className="share-list-privacy-option-switch-label">Permitir que los miembros inviten</span>
                  <Switch
                    checked={listMembersCanInvite}
                    onClick={(e) => {
                      e.stopPropagation();
                      void onPrivacyChange(listPrivacy, listMembersCanEdit, !listMembersCanInvite);
                    }}
                    aria-label="Permitir que los miembros inviten a otras personas al grupo"
                  />
                </div>
              )}
              {(listPrivacy === "public" || listPrivacy === "invitation") && (
                <div
                  className="share-list-privacy-option-switch-row create-list-privacy-option-switch-row"
                  onClick={(e) => e.stopPropagation()}
                >
                  <span className="share-list-privacy-option-switch-label">Permitir editar lista</span>
                  <Switch
                    checked={listMembersCanEdit}
                    onClick={(e) => {
                      e.stopPropagation();
                      void onPrivacyChange(listPrivacy, !listMembersCanEdit, listMembersCanInvite);
                    }}
                    aria-label="Permitir que los miembros añadan o quiten cafés de la lista"
                  />
                </div>
              )}
            </div>
          </div>
        )}

        {isOwner && list && (
          <div className="list-options-page-section">
            <ListOptionsMembersBlock
              variant="page"
              listOwnerId={list.user_id}
              members={listOptionsMembers}
              memberUsers={listOptionsMemberUsers}
              users={users}
              currentUserId={currentUserId}
              shareUrl={shareUrl}
              invitingId={invitingId}
              onInvite={onInvite}
              onCopyLink={onCopyLink}
              onRemoveMember={onRemoveMember}
              copyChipVisible={showCopyChip}
              copyChipExiting={copyChipExiting}
              invitations={invitations}
              canRemoveMember
            />
          </div>
        )}

        {!isOwner && list && listPrivacy === "public" && (
          <div className="list-options-page-section">
            <ListOptionsMembersBlock
              variant="page"
              listOwnerId={list.user_id}
              members={listOptionsMembers}
              memberUsers={listOptionsMemberUsers}
              users={users}
              currentUserId={currentUserId}
              shareUrl={shareUrl}
              invitingId={invitingId}
              onInvite={onInvite}
              onCopyLink={onCopyLink}
              onRemoveMember={async () => {}}
              copyChipVisible={showCopyChip}
              copyChipExiting={copyChipExiting}
              invitations={invitations}
              canRemoveMember={false}
              visibleMemberIds={mutualFollowIds}
            />
          </div>
        )}

        {!isOwner && list && listPrivacy === "invitation" && (list.members_can_invite === true) && (
          <div className="list-options-page-section">
            <ListOptionsMembersBlock
              variant="page"
              listOwnerId={list.user_id}
              members={listOptionsMembers}
              memberUsers={listOptionsMemberUsers}
              users={users}
              currentUserId={currentUserId}
              shareUrl={shareUrl}
              invitingId={invitingId}
              onInvite={onInvite}
              onCopyLink={onCopyLink}
              onRemoveMember={async () => {}}
              copyChipVisible={showCopyChip}
              copyChipExiting={copyChipExiting}
              invitations={invitations}
              canRemoveMember={false}
            />
          </div>
        )}

        <div className="list-options-page-section list-options-general">
          <h3 className="create-list-privacy-subtitle">General</h3>
          <div className="list-options-general-card">
            {isOwner ? (
              <>
                <Button
                  variant="plain"
                  className="list-options-page-action list-options-page-action-edit"
                  onClick={onEditList}
                >
                  <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">edit</span>
                  <span>Editar lista</span>
                  <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                </Button>
                <Button
                  variant="plain"
                  className="list-options-page-action list-options-page-action-delete"
                  onClick={onDeleteList}
                >
                  <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">delete</span>
                  <span>Eliminar lista</span>
                  <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                </Button>
              </>
            ) : (
              <Button
                variant="plain"
                className="list-options-page-action list-options-page-action-leave list-options-page-action-leave-row"
                onClick={onLeaveList}
              >
                <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">logout</span>
                <span>Salir de la lista</span>
              </Button>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}

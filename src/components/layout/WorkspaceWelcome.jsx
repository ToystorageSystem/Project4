import { useAuth } from "../../auth/AuthContext";
import { roleLabel } from "../../auth/access";
import { displayName, useWorkspaceClock } from "../../hooks/useWorkspacePersonalization";
import WorkspaceIcon from "../ui/WorkspaceIcon";
export default function WorkspaceWelcome() {
  const { user } = useAuth();
  const clock = useWorkspaceClock();
  return <section className={`personal-welcome period-${clock.key}`}><div><h1>{clock.greeting}, <span>{displayName(user)}.</span></h1><div className="welcome-context"><span>{roleLabel(user)}</span></div></div><div className="day-clock"><WorkspaceIcon name={clock.icon} size={34}/><div><time dateTime={clock.now.toISOString()}>{clock.now.toLocaleTimeString("en-GB",{hour:"2-digit",minute:"2-digit"})}</time><span>{clock.now.toLocaleDateString("en-GB",{weekday:"short",day:"numeric",month:"long"})}</span></div></div></section>;
}

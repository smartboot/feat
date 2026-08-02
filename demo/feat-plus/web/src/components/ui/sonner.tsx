import { Toaster as Sonner, type ToasterProps } from "sonner"
export function Toaster(props: ToasterProps) { return <Sonner className="toaster group" toastOptions={{ classNames: { toast: "group toast group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg", description: "group-[.toast]:text-muted-foreground" } }} {...props} /> }

import { AppShell, Burger, Container, Group, NavLink, Title } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import ProjectsPage from "./pages/ProjectsPage";
import TimeEntriesPage from "./pages/TimeEntriesPage";

export default function App() {
  const [navOpened, { toggle: toggleNav, close: closeNav }] = useDisclosure(false);
  const location = useLocation();

  const navItems = [
    { to: "/projects", label: "Projects" },
    { to: "/time-entries", label: "Time Entries" },
  ];

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{
        width: 200,
        breakpoint: "sm",
        collapsed: { mobile: !navOpened },
      }}
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group>
            <Burger opened={navOpened} onClick={toggleNav} hiddenFrom="sm" size="sm" />
            <Title order={3}>Time Tracker</Title>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            label={item.label}
            component="button"
            active={location.pathname.startsWith(item.to)}
            onClick={() => {
              closeNav();
              window.history.pushState({}, "", item.to);
              window.dispatchEvent(new PopStateEvent("popstate"));
            }}
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main>
        <Container size="lg" py="md">
          <Routes>
            <Route path="/projects" element={<ProjectsPage />} />
            <Route path="/time-entries" element={<TimeEntriesPage />} />
            <Route path="*" element={<Navigate to="/projects" replace />} />
          </Routes>
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}


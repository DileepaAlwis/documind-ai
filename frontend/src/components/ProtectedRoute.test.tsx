import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { describe, expect, it, vi } from "vitest";
import type { AuthStatus } from "../context/AuthContext";
import ProtectedRoute from "./ProtectedRoute";

const mockUseAuth = vi.hoisted(() => vi.fn());
vi.mock("../context/useAuth", () => ({ useAuth: () => mockUseAuth() }));

function renderAt(status: AuthStatus) {
  mockUseAuth.mockReturnValue({ status, user: null });
  return render(
    <MemoryRouter initialEntries={["/secret"]}>
      <Routes>
        <Route path="/login" element={<div>login page</div>} />
        <Route element={<ProtectedRoute />}>
          <Route path="/secret" element={<div>secret content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe("ProtectedRoute", () => {
  it("redirects anonymous visitors to the login page", () => {
    renderAt("anonymous");
    expect(screen.getByText("login page")).toBeInTheDocument();
    expect(screen.queryByText("secret content")).not.toBeInTheDocument();
  });

  it("shows a spinner and no content while the session is being restored", () => {
    renderAt("loading");
    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.queryByText("secret content")).not.toBeInTheDocument();
  });

  it("renders the protected page when authenticated", () => {
    renderAt("authenticated");
    expect(screen.getByText("secret content")).toBeInTheDocument();
  });
});

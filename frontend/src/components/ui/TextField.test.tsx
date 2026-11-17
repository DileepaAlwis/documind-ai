import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import TextField from "./TextField";

describe("TextField", () => {
  it("connects the label to the input", () => {
    render(<TextField label="Email" />);
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
  });

  it("shows the error message and marks the field invalid", () => {
    render(<TextField label="Email" error="Email is required." />);
    expect(screen.getByRole("alert")).toHaveTextContent("Email is required.");
    expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
  });

  it("toggles password visibility", () => {
    render(<TextField label="Password" type="password" />);
    const input = screen.getByLabelText("Password");
    expect(input).toHaveAttribute("type", "password");

    fireEvent.click(screen.getByRole("button", { name: "Show password" }));
    expect(input).toHaveAttribute("type", "text");

    fireEvent.click(screen.getByRole("button", { name: "Hide password" }));
    expect(input).toHaveAttribute("type", "password");
  });
});

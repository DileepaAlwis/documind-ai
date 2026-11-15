import { describe, expect, it } from "vitest";
import {
  validateEmail,
  validateFullName,
  validatePassword,
  validatePasswordConfirmation,
} from "./validation";

describe("validateEmail", () => {
  it("accepts a normal address and ignores surrounding spaces", () => {
    expect(validateEmail("  alice@example.com ")).toBeNull();
  });
  it("rejects empty and malformed addresses", () => {
    expect(validateEmail("")).not.toBeNull();
    expect(validateEmail("alice")).not.toBeNull();
    expect(validateEmail("alice@")).not.toBeNull();
    expect(validateEmail("a b@example.com")).not.toBeNull();
  });
});

describe("validatePassword", () => {
  it("accepts 8+ characters with a letter and a number", () => {
    expect(validatePassword("Passw0rd123")).toBeNull();
  });
  it("rejects short, letters-only and numbers-only passwords", () => {
    expect(validatePassword("")).not.toBeNull();
    expect(validatePassword("Ab1")).not.toBeNull();
    expect(validatePassword("onlyletters")).not.toBeNull();
    expect(validatePassword("12345678")).not.toBeNull();
  });
  it("rejects passwords longer than 72 characters", () => {
    expect(validatePassword("a1".repeat(37))).not.toBeNull();
  });
});

describe("validateFullName", () => {
  it("requires a non-blank name", () => {
    expect(validateFullName("   ")).not.toBeNull();
    expect(validateFullName("Alice")).toBeNull();
  });
});

describe("validatePasswordConfirmation", () => {
  it("requires a matching confirmation", () => {
    expect(validatePasswordConfirmation("Passw0rd123", "")).not.toBeNull();
    expect(validatePasswordConfirmation("Passw0rd123", "Different1")).not.toBeNull();
    expect(validatePasswordConfirmation("Passw0rd123", "Passw0rd123")).toBeNull();
  });
});

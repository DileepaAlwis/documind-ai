import axios from "axios";
import type { ApiError } from "../types/api";

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (error.response) {
      const data = error.response.data as Partial<ApiError> | undefined;
      return data?.message ?? `Request failed (${error.response.status}).`;
    }
    return "Cannot reach the server. Check that the API is running.";
  }
  return "Something went wrong.";
}

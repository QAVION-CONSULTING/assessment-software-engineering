import { describe, expect, it } from "vitest";
import {
  fmtEur,
  fmtHours,
} from "../pages/ProjectsPage";

describe("fmtEur / fmtHours", () => {
  it("formats cents as EUR and minutes as hours (de-DE)", () => {
    expect(fmtEur(960000)).toMatch(/9\.600,00/);
    expect(fmtHours(4950)).toMatch(/82,5/);
  });
});
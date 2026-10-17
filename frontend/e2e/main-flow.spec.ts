import { expect, test } from "@playwright/test";

test("create project, log time, see budget usage update", async ({ page }) => {
  const name = `E2E Project ${Date.now()}`;

  // 1. Create a project
  await page.goto("/projects");
  await page.getByRole("button", { name: "New project" }).click();
  await page.getByLabel(/^Name/).fill(name);
  await page.getByLabel(/^Time budget/).fill("10");
  await page.getByLabel(/^Cost budget/).fill("1000");
  await page.getByLabel(/^Hourly rate/).fill("80");
  await page.getByRole("button", { name: "Create" }).click();

  const row = page.getByRole("row", { name: new RegExp(name) });
  await expect(row).toBeVisible();
  await expect(row.getByText("ok")).toBeVisible();

  // 2. Log 2 h against it
  await page.getByRole("button", { name: "Time Entries" }).click();
  await page.getByRole("button", { name: "New entry" }).click();
  await page.getByLabel(/^Project/).selectOption({ label: name });
  await page.getByLabel(/^Duration/).fill("2");
  await page.getByRole("button", { name: "Create" }).click();

  await expect(page.getByRole("cell", { name })).toBeVisible();

  // 3. Back on projects: usage shows 2 h / 10 h
  await page.getByRole("button", { name: "Projects" }).click();
  await expect(row).toContainText("2 h / 10 h");
});

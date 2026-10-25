import os
import sys
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

def main():
    out_dir = os.path.abspath("deliverables/evidence/screenshots")
    os.makedirs(out_dir, exist_ok=True)
    print(f"Target screenshots directory: {out_dir}")

    chrome_options = Options()
    chrome_options.add_argument("--headless=new")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--window-size=1440,1050")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")

    driver = webdriver.Chrome(options=chrome_options)
    wait = WebDriverWait(driver, 10)

    try:
        # 1. Main Search Interface / Homepage
        print("Navigating to http://localhost:8080/...")
        driver.get("http://localhost:8080/")
        time.sleep(2)
        
        # Wait for stats to load
        wait.until(EC.text_to_be_present_in_element((By.ID, "statTotalDocs"), "50,000"))
        time.sleep(1)
        driver.save_screenshot(os.path.join(out_dir, "01_main_search_interface.png"))
        print("Captured: 01_main_search_interface.png")

        # 2. Autocomplete Suggestions (Compressed Trie)
        query_input = driver.find_element(By.ID, "queryInput")
        query_input.clear()
        query_input.send_keys("crick")
        time.sleep(1.5)
        driver.save_screenshot(os.path.join(out_dir, "02_trie_autocomplete_dropdown.png"))
        print("Captured: 02_trie_autocomplete_dropdown.png")

        # 3. Search Results with Matched-Term Explanations (Coverage-Aware TF-IDF)
        query_input.clear()
        query_input.send_keys("quantum cosmology")
        search_btn = driver.find_element(By.ID, "searchBtn")
        search_btn.click()
        time.sleep(2)
        driver.save_screenshot(os.path.join(out_dir, "03_coverage_search_results.png"))
        print("Captured: 03_coverage_search_results.png")

        # 4. Search Evaluation Metrics Panel
        eval_toggle = driver.find_element(By.ID, "evalToggleBtn")
        driver.execute_script("arguments[0].scrollIntoView(true);", eval_toggle)
        time.sleep(0.5)
        eval_toggle.click()
        time.sleep(1)
        # Click calculate/evaluate button in the panel if present
        try:
            run_eval_btn = driver.find_element(By.ID, "runEvalBtn")
            run_eval_btn.click()
            time.sleep(1)
        except Exception as e:
            print("runEvalBtn not clicked:", e)
        driver.save_screenshot(os.path.join(out_dir, "04_search_quality_evaluation.png"))
        print("Captured: 04_search_quality_evaluation.png")
        # Close or scroll back up
        eval_toggle.click()
        time.sleep(0.5)

        # 5. Okapi BM25 Ranking Mode
        driver.execute_script("window.scrollTo(0, 0);")
        time.sleep(0.5)
        bm25_btn = driver.find_element(By.CSS_SELECTOR, "button[data-algo='bm25']")
        bm25_btn.click()
        time.sleep(0.5)
        driver.find_element(By.ID, "searchBtn").click()
        time.sleep(2)
        driver.save_screenshot(os.path.join(out_dir, "05_okapi_bm25_results.png"))
        print("Captured: 05_okapi_bm25_results.png")

        # 6. Semantic Search Mode (128-D LSA)
        driver.execute_script("window.scrollTo(0, 0);")
        time.sleep(0.5)
        sem_btn = driver.find_element(By.CSS_SELECTOR, "button[data-algo='semantic']")
        sem_btn.click()
        time.sleep(0.5)
        query_input = driver.find_element(By.ID, "queryInput")
        query_input.clear()
        query_input.send_keys("cardiac surgery")
        driver.find_element(By.ID, "searchBtn").click()
        time.sleep(2)
        driver.save_screenshot(os.path.join(out_dir, "06_semantic_lsa_results.png"))
        print("Captured: 06_semantic_lsa_results.png")

        # 7. Three-Way Ranking Comparison Lab
        driver.execute_script("window.scrollTo(0, 0);")
        time.sleep(0.5)
        compare_btn = driver.find_element(By.ID, "compareAllBtn")
        compare_btn.click()
        time.sleep(0.5)
        query_input = driver.find_element(By.ID, "queryInput")
        query_input.clear()
        query_input.send_keys("heart failure clinical trial")
        driver.find_element(By.ID, "searchBtn").click()
        time.sleep(3)
        driver.save_screenshot(os.path.join(out_dir, "07_three_way_comparison_lab.png"))
        print("Captured: 07_three_way_comparison_lab.png")

        # 8. Research Workspace
        ws_nav = driver.find_element(By.ID, "workspaceNavBtn")
        driver.execute_script("window.scrollTo(0, 0);", ws_nav)
        time.sleep(0.5)
        ws_nav.click()
        time.sleep(1.5)
        driver.save_screenshot(os.path.join(out_dir, "08_research_workspace.png"))
        print("Captured: 08_research_workspace.png")

        # Close workspace modal/view
        try:
            close_ws = driver.find_element(By.CSS_SELECTOR, "#workspaceModal .modal-close, #workspaceModal .btn-close, .close-modal")
            close_ws.click()
            time.sleep(0.5)
        except Exception:
            driver.get("http://localhost:8080/")
            time.sleep(1.5)

        # 9. Live Search Analytics Dashboard
        analytics_nav = driver.find_element(By.ID, "analyticsNavBtn")
        analytics_nav.click()
        time.sleep(2)
        driver.save_screenshot(os.path.join(out_dir, "09_live_search_analytics.png"))
        print("Captured: 09_live_search_analytics.png")

        # Close analytics modal/view
        try:
            close_an = driver.find_element(By.CSS_SELECTOR, "#analyticsModal .modal-close, #analyticsModal .btn-close, .close-modal")
            close_an.click()
            time.sleep(0.5)
        except Exception:
            driver.get("http://localhost:8080/")
            time.sleep(1.5)

        # 10. Google Comparison Lab
        google_nav = driver.find_element(By.ID, "googleComparisonNavBtn")
        google_nav.click()
        time.sleep(2)
        driver.save_screenshot(os.path.join(out_dir, "10_google_comparison_lab.png"))
        print("Captured: 10_google_comparison_lab.png")

        # 11. Architecture & Data Structures Modal
        try:
            close_g = driver.find_element(By.CSS_SELECTOR, "#googleComparisonModal .modal-close, #googleComparisonModal .btn-close, .close-modal")
            close_g.click()
            time.sleep(0.5)
        except Exception:
            driver.get("http://localhost:8080/")
            time.sleep(1.5)

        arch_nav = driver.find_element(By.ID, "archModalBtn")
        arch_nav.click()
        time.sleep(1.5)
        driver.save_screenshot(os.path.join(out_dir, "11_system_architecture_modal.png"))
        print("Captured: 11_system_architecture_modal.png")

        print("All screenshots successfully captured!")

    finally:
        driver.quit()

if __name__ == "__main__":
    main()

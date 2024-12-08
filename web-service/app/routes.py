import time

import requests
from flask import Blueprint, render_template, request, redirect, url_for
from datetime import datetime
import pytz
import csv

main_bp = Blueprint('main', __name__)


@main_bp.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        # Pobierz dane z formularza
        person_name = request.form['name']
        person_email = request.form['email']
        carrental_brand = request.form['car_brand']
        carrental_model = request.form['car_model']
        carrental_country = request.form['car_country']
        carrental_start = request.form['parking_start']
        carrental_end = request.form['parking_end']
        parking1_name = request.form['parking1_name']
        parking2_name = request.form['parking2_name']
        payment_card_name = request.form['payment_card_name']
        payment_card_valid_to = request.form['payment_card_valid_to']
        payment_card_number = request.form['payment_card_number']

        # Przekształcanie do datetime obiektu
        start_time = datetime.strptime(carrental_start, '%Y-%m-%dT%H:%M')
        start_time = pytz.UTC.localize(start_time)
        start_time_str = start_time.isoformat()

        end_time = datetime.strptime(carrental_end, '%Y-%m-%dT%H:%M')
        end_time = pytz.UTC.localize(end_time)
        end_time_str = end_time.isoformat()

        # Wstawienie do payload
        data = {
            "person": {
                "name": person_name,
                "email": person_email
            },
            "car": {
                "brand": carrental_brand,
                "model": carrental_model,
                "country": carrental_country,
                "city": parking1_name,
                "checkIn": start_time_str,
                "checkOut": end_time_str
            },
            "parking": {
                "from": {
                    "airport": parking1_name,
                    "date": start_time_str
                },
                "to": {
                    "airport": parking2_name,
                    "date": end_time_str
                }
            },
            "paymentCard": {
                "name": payment_card_name,
                "validTo": payment_card_valid_to,
                "number": payment_card_number
            }
        }

        # Wykonaj żądanie POST
        url = "http://localhost:8090/api/carrental/booking"
        url = "http://gateway:8090/api/carrental/booking"
        headers = {
            "accept": "application/json",
            "Content-Type": "application/json"
        }

        try:
            response = requests.post(url, json=data, headers=headers)
            response.raise_for_status()  # Podnosi wyjątek, jeśli status jest inny niż 2xx
            dane = get_booking_data()
            return render_template('index.html', success=True, dane=dane)
        except requests.exceptions.RequestException as e:
            return render_template('index.html', success=False, dane=None, error=str(e))

    return render_template('index.html', success=True, dane=None)  # Dopisałem sukces True


def get_booking_data():
    csv_path = '/app/data/carrental_notifications.csv'
    time.sleep(0.2)
    try:
        with open(csv_path, mode='r', encoding='utf-8') as file:
            csv_reader = list(csv.reader(file, delimiter=';'))
            if len(csv_reader) > 1:
                latest_record = csv_reader[-1]
                id_value = latest_record[0].strip(' "')
                cost_value = latest_record[1].strip(' "')
                print(f"id: {id_value}, cost: {cost_value}")
                return {"id": id_value, "cost": cost_value}
    except:
        pass

    return 404
